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

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.ai.engines.Ollama;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.transform.IdentityTransformer;
import com.invirgance.convirgance.web.binding.Binding;
import com.invirgance.convirgance.web.http.HttpRequest;
import com.invirgance.convirgance.web.servlet.ServiceState;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import java.util.List;

/**
 *
 * @author jbanes
 */
@Wiring("ollama-start-conversation")
public class OllamaConversationInitializer implements ChatModel
{
    private boolean stream = false; // Default to not stream
    private boolean raw = false;
    
    private String model = "llama3.2";
    private String chat;
    private String system;
    private String template;

    private String sessionKey = "conversation";
    private List<String> requiredSession;
    private List<String> optionalSession;
    
    private Ollama engine = new Ollama();
    
    public Ollama getEngine()
    {
        return engine;
    }

    public String getBaseUrl()
    {
        return engine.getBaseUrl();
    }

    public void setBaseUrl(String baseUrl)
    {
        this.engine.setBaseUrl(baseUrl);
    }
    
    public boolean isStream()
    {
        return stream;
    }

    public void setStream(boolean stream)
    {
        this.stream = stream;
    }

    public boolean isRaw()
    {
        return raw;
    }

    public void setRaw(boolean raw)
    {
        this.raw = raw;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public String getChat()
    {
        return chat;
    }

    public void setChat(String chat)
    {
        this.chat = chat;
    }

    public String getSystem()
    {
        return system;
    }

    public void setSystem(String system)
    {
        this.system = system;
    }

    public String getTemplate()
    {
        return template;
    }

    public void setTemplate(String template)
    {
        this.template = template;
    }

    public String getSessionKey()
    {
        return sessionKey;
    }

    public void setSessionKey(String sessionKey)
    {
        this.sessionKey = sessionKey;
    }

    public List<String> getRequiredSession()
    {
        return requiredSession;
    }

    public void setRequiredSession(List<String> requiredSession)
    {
        this.requiredSession = requiredSession;
    }

    public List<String> getOptionalSession()
    {
        return optionalSession;
    }

    public void setOptionalSession(List<String> optionalSession)
    {
        this.optionalSession = optionalSession;
    }
    
    @Override
    public Iterable<JSONObject> getBinding(JSONObject parameters)
    {
        var request = (HttpRequest)ServiceState.get("request");
        var session = request.getSession();
        var conversation = new JSONArray();
        
        var chat = template(this.chat, parameters);
        var system = this.system == null ? null : template(this.system, parameters);
        var template = this.template == null ? null : template(this.template, parameters);
        
        Iterable<JSONObject> iterable;
        
        session.setAttribute(sessionKey, conversation);
        
        if(requiredSession != null)
        {
            for(var key : requiredSession)
            {
                if(!parameters.containsKey(key)) throw new ConvirganceException("Key [" + key + "] is required to initialize the session. Double check that it is setup in the parameters.");

                session.setAttribute(key, parameters.get(key));
            }
        }
        
        if(optionalSession != null)
        {
            for(var key : optionalSession)
            {
                if(parameters.containsKey(key))
                {
                    session.setAttribute(key, parameters.get(key));
                }
            }
        }
        
        iterable = engine.generate(model, chat, stream, null, null, null, system, template, raw);
        
        return new IdentityTransformer() {
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                session.setAttribute(sessionKey, record.get("context"));
                
                return record;
            }
        }.transform(iterable);
    }
    
}
