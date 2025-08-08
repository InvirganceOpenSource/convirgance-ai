/*
 * The MIT License
 *
 * Copyright 2025 INVIRGANCE LLC.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.invirgance.convirgance.ai.vector;

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.ai.Document;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.web.http.HttpRequest;
import com.invirgance.convirgance.web.servlet.ServiceState;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;

/**
 *
 * @author jbanes
 */
@Wiring
public class MarkdownDocument implements Document
{
    private String path;

    public MarkdownDocument()
    {
    }

    public String getPath()
    {
        return path;
    }

    public void setPath(String path)
    {
        this.path = path;
    }
    
    private void compact(Block block)
    {
        if(block.isLeaf()) return;
        
        for(var child : block.children) compact(child);
        
        if(!block.canCompact()) return;

        for(int i=1; i<block.children.size(); i++)
        {
            if(!block.children.get(i-1).isLeaf()) continue;
            if(!block.children.get(i).isLeaf()) continue;

            block.children.get(i-1).paragraph += "\n\n" + block.children.get(i).paragraph;
            block.children.remove(i--);
        }
    }
    
    private void consolidate(Block block, JSONArray<String> output)
    {
        var buffer = new StringBuffer();
        
        if(!block.isLeaf())
        {
            for(var child : block.children)
            {
                consolidate(child, output);
            }
            
            return;
        }
        
        buffer.insert(0, block.paragraph);

        while(block.parent != null)
        {
            block = block.parent;

            buffer.insert(0, "\n\n");
            buffer.insert(0, block.paragraph);
        }

        output.add(buffer.toString());
    }
    
    private JSONArray<String> documentize(String filename, JSONArray<String> input)
    {
        JSONArray<String> output = new JSONArray<>();
        Block root = new Block("File: " + filename, null, 0, new JSONArray<>());
        Block parent = root;
        
        Block block;
        int depth;
        
        for(String paragraph : input)
        {
            if(paragraph.charAt(0) == '#')
            {
                depth = 0;
                
                while(depth < paragraph.length() && paragraph.charAt(depth) == '#')
                {
                    depth++;
                }
                
                while(depth <= parent.depth) parent = parent.parent;
                
                block = new Block(paragraph, parent, depth, new JSONArray<>());

                parent.children.add(block);
                
                parent = block;
            }
            else
            {
                parent.children.add(new Block(paragraph, parent, parent.depth+1, new JSONArray<>()));
            }
        }
        
        compact(root);
        consolidate(root, output);
        
        return output;
    }
    
    private boolean codeMarker(StringBuffer buffer)
    {
        if(buffer.length() < 3) return false;
        
        for(int i=1; i<=3; i++)
        {
            if(buffer.charAt(buffer.length()-i) != '`') return false;
        }
        
        return true;
    }
    
    private String normalize(StringBuffer buffer)
    {
        int index = 0;
        int end;
        
        if(buffer.indexOf("```") >= 0) return buffer.toString().trim();
        
        while((index = buffer.indexOf("<!--", index)) >= 0)
        {
            end = buffer.indexOf("-->", index);

            if(end > index) buffer.replace(index, end, "");
            else index = buffer.length();
        }
        
        return buffer.toString().trim();
    }
    
    private JSONArray<String> parse(File file)
    {
        var paragraphs = new JSONArray<String>();
        var paragraph = new StringBuffer();
        
        String normalized;
        boolean code = false;
        int c;
        
        System.out.println("Parsing " + file.getPath() + "...");
        
        try(var reader = new BufferedReader(new InputStreamReader(new FileInputStream(file))))
        {
            if(codeMarker(paragraph))
            {
                code = !code;
                
                if(code)
                {
                    // Ensure the code has context
                    paragraph.insert(0, "\n\n");
                    paragraph.insert(0, paragraphs.remove((int)paragraphs.size()-1));
                }
            }
            
            while((c = reader.read()) > 0)
            {
                if(c == '\r')
                {
                    continue;
                }
                else if(c == '\n')
                {
                    if(!code && paragraph.length() > 0 && paragraph.charAt(paragraph.length()-1) == '\n')
                    {
                        normalized = normalize(paragraph);
                        
                        if(normalized.length() > 0) paragraphs.add(normalized);
                        
                        paragraph.setLength(0);
                    }
                    else
                    {
                        paragraph.append((char)c);
                    }
                }
                else
                {
                    paragraph.append((char)c);
                }
            }
        }
        catch(IOException e)
        {
            throw new ConvirganceException(e);
        }
        
        if(paragraph.length() > 0) paragraphs.add(paragraph.toString());
        
        return documentize(file.getName(), paragraphs);
    }
    
    private JSONArray<String> parseDirectory(File directory)
    {
        var results = new JSONArray<String>();
        
        for(var file : directory.listFiles())
        {
            if(file.isDirectory()) results.addAll(parseDirectory(directory));
            else if(file.getName().toLowerCase().endsWith(".md")) results.addAll(parse(file));
        }
        
        return results;
    }
    
    @Override
    public Iterator<String> iterator()
    {
        var request = (HttpRequest)ServiceState.get("request");
        var file = request.getFileByPath(path);

        if(file.isDirectory()) return parseDirectory(file).iterator();
        
        return parse(file).iterator();
    }
    
    private class Block
    {
        String paragraph;
        Block parent;
        int depth;
        JSONArray<Block> children;

        public Block(String paragraph, Block parent, int depth, JSONArray<Block> children)
        {
            this.paragraph = paragraph;
            this.parent = parent;
            this.depth = depth;
            this.children = children;
        }
        
        public boolean isLeaf()
        {
            return (children.size() < 1);
        }
        
        public boolean canCompact()
        {
            int count = 0;
            int max = 0;
            
            for(var child : children)
            {
                if(child.isLeaf()) count++;
                else count = 0;
                
                if(count > max) max = count;
            }
            
            return (max > 1);
        }
    }
}
