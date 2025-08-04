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
package com.invirgance.convirgance.ai;

import com.invirgance.convirgance.ai.advisors.OllamaConversationAdvisor;
import com.invirgance.convirgance.ai.tools.ComputeTool;
import com.invirgance.convirgance.json.JSONObject;
import java.util.List;
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
public class OllamaChatModelTest
{
    
    public OllamaChatModelTest()
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
    public void testTemplate()
    {
        var model = new OllamaChatModel();
        var parameters = new JSONObject();
        
        parameters.put("color", "blue");
        
        System.out.println("Prompt: " + model.template("Why is the sky ${color}", parameters));
    }
    
    @Test
    public void testBinding()
    {
        var model = new OllamaChatModel();
        var parameters = new JSONObject();
        
        parameters.put("color", "orange");
        model.setChat("Why is the sky ${color}?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.println(record.toString(4));
        }
    }
    
    @Test
    public void testConversation()
    {
        var model = new OllamaChatModel();
        var parameters = new JSONObject();
        
        model.setAdvisors(List.of(new OllamaConversationAdvisor()));
        parameters.put("conversation", "1");
        parameters.put("talkLike", "Comedian");
        model.setSystem("Talk like ${talkLike}");
        model.setOptions(new JSONObject("{\"temperature\": 1.0}"));
        
        parameters.put("color", "blue");
        model.setChat("Why is the sky ${color}?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.print(record.getString("response"));
        }
        
        System.out.println("\n-----\n");
        
        parameters.put("color", "orange");
        model.setChat("Why is it ${color}?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.print(record.getString("response"));
        }
        
        System.out.println("\n-----\n");
        
        parameters.put("color", "checkered");
        model.setChat("Can it be ${color}?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.print(record.getString("response"));
        }
        
        System.out.println("\n-----\n");
    }
    
    @Test
    public void testInstructions()
    {
        var model = new OllamaChatModel();
        var parameters = new JSONObject();
        
        model.setAdvisors(List.of(new OllamaConversationAdvisor()));
        parameters.put("conversation", "1");
        parameters.put("talkLike", "Rodney Dangerfield");
        model.setSystem("Talk like ${talkLike}");
        model.setOptions(new JSONObject("{\"temperature\": 1.2}"));
        
        model.setChat("Tell me a joke");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.print(record.getString("response"));
        }
        
        System.out.println("\n------\n");
        
        for(int i=0; i<4; i++)
        {
            model.setChat("Tell me another joke");

            for(var record : model.getBinding(parameters))
            {
                System.out.print(record.getString("response"));
            }
        
            System.out.println("\n------\n");
        }
    }
    
    @Test
    public void testTool()
    {
        var model = new OllamaChatModel();
        var parameters = new JSONObject();
        
        model.setAdvisors(List.of(new OllamaConversationAdvisor()));
        model.setTools(List.of(new ComputeTool()));
        
        parameters.put("conversation", "1");
        
//        model.setChat("What is ((3.14159 * 3) + 12) / 3.2?");
        model.setChat("What is 3 + 2?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.println(record.toString(4));
        }
        
        model.setChat("What is 9 / 3?");
        
        for(var record : model.getBinding(parameters))
        {
            System.out.println(record.toString(4));
        }
    }
    
}
