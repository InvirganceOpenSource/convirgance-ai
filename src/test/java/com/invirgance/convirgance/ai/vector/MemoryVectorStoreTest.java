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

import com.invirgance.convirgance.ai.engines.Ollama;
import com.invirgance.convirgance.json.JSONArray;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author jbanes
 */
public class MemoryVectorStoreTest
{
    
    public MemoryVectorStoreTest()
    {
    }
    
    @BeforeAll
    public static void setUpClass()
    {
    }
    
    @AfterAll
    public static void tearDownClass()
    {
    }
    
    @BeforeEach
    public void setUp()
    {
    }
    
    @AfterEach
    public void tearDown()
    {
    }

    @Test
    public void testComputeDotProduct()
    {
        var left = new JSONArray<Double>("[-7,4]");
        var right = new JSONArray<Double>("[-6,5]");
        var sum = MemoryVectorStore.computeDotProduct(left, right);
        
        assertEquals(62.0, sum);
    }

    @Test
    public void testComputeMagnitude()
    {
        assertEquals(5.0, MemoryVectorStore.computeMagnitude(new JSONArray<Double>("[3,4]")));
        assertEquals(Math.sqrt(65), MemoryVectorStore.computeMagnitude(new JSONArray<Double>("[-7,4]")));
        assertEquals(Math.sqrt(61), MemoryVectorStore.computeMagnitude(new JSONArray<Double>("[-6,5]")));
    }

    @Test
    public void testComputeCosineSimilarity()
    {
        assertEquals((3/6.164414), MemoryVectorStore.computeCosineSimilarity(new JSONArray("[3,2,0,5]"), new JSONArray("[1,0,0,0]")), 0.01);
        assertEquals(0.9846, MemoryVectorStore.computeCosineSimilarity(new JSONArray("[-7,4]"), new JSONArray("[-6,5]")), 0.01);
        assertEquals(1.0, MemoryVectorStore.computeCosineSimilarity(new JSONArray("[7,4]"), new JSONArray("[7,4]")), 0.01);
        assertEquals(-0.50769, MemoryVectorStore.computeCosineSimilarity(new JSONArray("[7,4]"), new JSONArray("[-7,4]")), 0.01);
        assertEquals(-1.0, MemoryVectorStore.computeCosineSimilarity(new JSONArray("[7,4]"), new JSONArray("[-7,-4]")), 0.01);
    }
    
    @Test
    public void testDocuments()
    {
        var documents = new String[]{
            "The sky is blue because of a magic spell cast by the space wizard Obi-Wan Kenobi",
            "Occasionally the sky turns green when Anikin Skywalker uses the dark side of the force",
            "What the sky is made of is cheese. Particularly a good brie."
        };
        
        var instance = new Ollama();
        var store = new MemoryVectorStore();
        var embeddings = instance.getEmbed("llama3.2", documents);
        
        var messages = new JSONArray();
        var question = "Why is the sky blue?";
        var document = "";
        
        for(int i=0; i<documents.length; i++)
        {
            store.register(embeddings.get(i), documents[i]);
        }

        for(var record : store.matches(instance.getEmbed("llama3.2", question)))
        {
            document += "\n=======\n";
            document += record.getString("document");
        }
        
        if(document.length() > 0)
        {
            messages.add(instance.constructMessage("You are a game master answering questions about a game. Do not mention that this a game. Do not try to correct this information. Stay in character and use the following document for game information.\n" + document, Ollama.Role.system));
        }
        
        messages.add(instance.constructMessage(question, Ollama.Role.user));
        
        for(var record : instance.chat("llama3.2", messages, false))
        {
            System.out.println(record.toString(4));
            messages.add(record.get("message"));
        }
        
        question = "Why is the sky green?";
        document = "";
        
        for(var record : store.matches(instance.getEmbed("llama3.2", question)))
        {
            document += "\n=======\n";
            document += record.getString("document");
        }
        
        if(document.length() > 0)
        {
            messages.add(instance.constructMessage("You are a game master answering questions about a game. Do not mention that this a game. Do not try to correct this information. Stay in character and use the following document for game information.\n" + document, Ollama.Role.system));
        }
        
        messages.add(instance.constructMessage(question, Ollama.Role.user));
        
        for(var record : instance.chat("llama3.2", messages, false))
        {
            System.out.println(record.toString(4));
            messages.add(record.get("message"));
        }
        
        question = "What is the sky made of?";
        document = "";
        
        for(var record : store.matches(instance.getEmbed("llama3.2", question)))
        {
            document += "\n=======\n";
            document += record.getString("document");
        }
        
        if(document.length() > 0)
        {
            messages.add(instance.constructMessage("You are a game master answering questions about a game. Do not mention that this a game. Do not try to correct this information. Stay in character and use the following document for game information.\n" + document, Ollama.Role.system));
        }
        
        messages.add(instance.constructMessage(question, Ollama.Role.user));
        
        for(var record : instance.chat("llama3.2", messages, false))
        {
            System.out.println(record.toString(4));
            messages.add(record.get("message"));
        }
    }
}
