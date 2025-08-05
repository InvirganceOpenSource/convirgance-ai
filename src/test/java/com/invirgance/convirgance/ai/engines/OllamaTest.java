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
package com.invirgance.convirgance.ai.engines;

import com.invirgance.convirgance.ai.vector.MemoryVectorStore;
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
public class OllamaTest
{
    
    public OllamaTest()
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
    public void testGenerate()
    {
        Ollama instance = new Ollama();
        
        for(var record : instance.generate("llama3.2", "How much wood would a woodchuck chuck if a woodchuck could chuck wood?", false))
        {
             System.out.println(record.toString(4));
        }
    }
    
    @Test
    public void testChat()
    {
        Ollama instance = new Ollama();

        var messages = new JSONArray();
        
        messages.add(instance.constructMessage("Talk like a pirate", Ollama.Role.system));
        messages.add(instance.constructMessage("How much wood would a woodchuck chuck if a woodchuck could chuck wood?", Ollama.Role.user));
        
        for(var record : instance.chat("llama3.2", messages, false))
        {
             System.out.println(record.toString(4));
        }
    }

    @Test
    public void testGetLoadedModels()
    {
        Ollama instance = new Ollama();
        
        System.out.println(instance.getLoadedModels().toString(4));
    }

    @Test
    public void testGetInstalledModels()
    {
        Ollama instance = new Ollama();
        
        System.out.println(instance.getInstalledModels().toString(4));
    }

    @Test
    public void testGetModelDetails()
    {
        Ollama instance = new Ollama();
        
        System.out.println(instance.getModelDetails("llama3.2").toString(4));
    }

    @Test
    public void testPullModel()
    {
        Ollama instance = new Ollama();
        
        for(var status : instance.pullModel("llama2:7b", true, false))
        {
            System.out.println(status.toString(4));
        }
    }

    @Test
    public void testDeleteModel()
    {
        Ollama instance = new Ollama();
        
        assertTrue(instance.deleteModel("llama2:7b"));
    }
    
    @Test
    public void testEmbeddings()
    {
        Ollama instance = new Ollama();
        
        var embeddings = instance.getEmbed("llama3.2", "Why is the sky blue?", "Why is the sky green?", "What is the sky made of?");
        var blue = embeddings.get(0);
        var green = embeddings.get(1);
        var material = embeddings.get(2);
        
        assertEquals(1.0000, MemoryVectorStore.computeCosineSimilarity(blue, blue), 0.01);
        assertEquals(0.7789, MemoryVectorStore.computeCosineSimilarity(blue, green), 0.01);
        assertEquals(0.8265, MemoryVectorStore.computeCosineSimilarity(blue, material), 0.01);
    }
}
