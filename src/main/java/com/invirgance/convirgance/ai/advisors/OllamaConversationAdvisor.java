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
package com.invirgance.convirgance.ai.advisors;

import com.invirgance.convirgance.ai.Advisor;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.web.http.HttpRequest;
import com.invirgance.convirgance.web.servlet.ServiceState;
import com.invirgance.convirgance.wiring.annotation.Wiring;

/**
 *
 * @author jbanes
 */
@Wiring("ollama-conversation")
public class OllamaConversationAdvisor implements Advisor
{
    private String sessionKey = "conversation";

    public String getSessionKey()
    {
        return sessionKey;
    }

    public void setSessionKey(String key)
    {
        this.sessionKey = key;
    }
    
    private JSONArray get()
    {
        var request = (HttpRequest)ServiceState.get("request");
        var session = request.getSession();
        
        return (JSONArray)session.getAttribute(sessionKey);
    }
    
    private void put(JSONArray context)
    {
        var request = (HttpRequest)ServiceState.get("request");
        var session = request.getSession();

        session.setAttribute(sessionKey, context);
    }
    
    @Override
    public void before(JSONObject parameters, JSONObject message)
    {
        JSONArray context = get();
        JSONArray messages;
        JSONArray sending;
        
// TODO: Require initialization?
//        if(context == null) return;
        
        if(message.containsKey("messages"))
        {
            messages = context == null ? new JSONArray() : (JSONArray)context;
            sending = message.getJSONArray("messages");
            
            // Sending tool responses. Need to remove the repeating request since we're
            // managing history.
            if(!messages.isEmpty() && messages.getJSONObject(messages.size()-1).containsKey("tool_calls"))
            {
                sending.remove(0);
            }
            
            for(JSONObject item : (JSONArray<JSONObject>)sending)
            {
                if(messages.isEmpty() || !messages.get(messages.size()-1).equals(item))
                {
                    messages.add(item);
                }
            }
            
            message.put("messages", messages);
            put(messages);
        }
        else if(context != null)
        {
            message.put("context", context);
        }
    }
    
    @Override
    public void after(JSONObject parameters, JSONObject message)
    {
        JSONArray messages;
        JSONObject result;
        JSONObject last;
        JSONArray context;
        
        if(message.containsKey("context"))
        {
            put(message.getJSONArray("context"));
        }
        else if(message.containsKey("message"))
        {
            context = get();
            
// TODO: Require initialization?
//        if(context == null) return;
            
            if(context == null)
            {
                messages = new JSONArray();
                
                messages.add(message.getJSONObject("message"));
            }
            else
            {
                messages = context;
                result = message.getJSONObject("message");
                last = !messages.isEmpty() ? messages.getJSONObject(messages.size()-1) : null;
                
                if(!messages.isEmpty() && last.getString("role").equals(result.getString("role")))
                {
                    last.put("content", last.getString("content") + result.getString("content"));
                }
                else
                {
                    messages.add(result);
                }
            }
        }
    }
}
