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
import com.invirgance.convirgance.input.JSONInput;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.Source;
import com.invirgance.convirgance.transform.IdentityTransformer;
import com.invirgance.convirgance.wiring.annotation.Wiring;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jbanes
 */
@Wiring("ollama-model")
public class OllamaChatModel implements ChatModel
{
    private String baseUrl = "http://localhost:11434/api";
    private boolean stream = false; // Default to not stream
    private boolean raw = false;
    
    private String model = "llama3.2";
    private String chat;
    private String system;
    private String template;
    
    private List<Advisor> advisors = new ArrayList<>();
    private List<Object> tools;
    private Map options;
    
    private OllamaToolEncoder encoder;

    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }

    public boolean getStream()
    {
        return stream;
    }

    public void setStream(boolean stream)
    {
        this.stream = stream;
    }

    public String getModel()
    {
        return model;
    }

    public void setModel(String model)
    {
        this.model = model;
    }

    public List<Advisor> getAdvisors()
    {
        return advisors;
    }

    public void setAdvisors(List<Advisor> advisors)
    {
        this.advisors = advisors;
    }

    public List<Object> getTools()
    {
        return tools;
    }

    public void setTools(List<Object> tools)
    {
        this.tools = tools;
        this.encoder = new OllamaToolEncoder(tools.toArray());
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

    public boolean isRaw()
    {
        return raw;
    }

    public void setRaw(boolean raw)
    {
        this.raw = raw;
    }

    public Map getOptions()
    {
        return options;
    }

    public void setOptions(Map options)
    {
        this.options = options;
    }

    private URL getUrl(String path)
    {
        try
        {
            if(!path.startsWith("/")) path = "/" + path;
            
            return new URI(baseUrl + path).toURL();
        }
        catch(URISyntaxException | MalformedURLException e) { throw new ConvirganceException(e); }
    }
    
    private JSONObject constructMessage(JSONObject parameters)
    {
        var message = new JSONObject();
        var messages = new JSONArray();
        var latest = new JSONObject();
        
        message.put("model", model);
        message.put("stream", stream);
        
        if(tools != null) 
        {
            latest.put("role", "user");
            latest.put("content", template(chat, parameters));
            messages.add(latest);
            message.put("messages", messages);
            message.put("tools", encoder.getDescriptors());
        }
        else
        {
            message.put("prompt", template(chat, parameters));
        }
        
        if(raw) message.put("raw", raw);
        if(system != null) message.put("system", template(system, parameters));
        if(template != null) message.put("template", template(template, parameters));
        if(options != null) message.put("options", options);
        
        for(Advisor advisor : advisors) advisor.before(parameters, message);

        return message;
    }
    
    private Iterable<JSONObject> getBinding(JSONObject parameters, JSONArray<JSONObject> toolCalls)
    {
        var api = "chat";
        var message = constructMessage(parameters);
        var messages = message.getJSONArray("messages");
        var source = new OllamaSource(api, message);
        var input = new JSONInput();
        
        JSONObject result;
       
        for(var call : toolCalls)
        {
            result = new JSONObject();
            
            result.put("role", "tool");
            result.put("content", encoder.execute(call));
            messages.add(result);
        }
        
        return new IdentityTransformer() {
            private Iterator<JSONObject> replacement;

            @Override
            public Iterator<JSONObject> transform(Iterator<JSONObject> iterator) throws ConvirganceException
            {
                var original = IdentityTransformer.super.transform(iterator);
                
                return new Iterator<JSONObject>() {
                    @Override
                    public boolean hasNext()
                    {
                        var iterator = (replacement != null) ? replacement : original;
                        
                        return iterator.hasNext();
                    }

                    @Override
                    public JSONObject next()
                    {
                        var iterator = (replacement != null) ? replacement : original;
                        
                        return iterator.next();
                    }
                };
            }
            
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                for(Advisor advisor : advisors) advisor.after(parameters, record);
                
                if(record.containsKey("message") && record.getJSONObject("message").containsKey("tool_calls"))
                {
                    replacement = getBinding(parameters, record.getJSONObject("message").getJSONArray("tool_calls")).iterator();
                    
                    return replacement.next();
                }
                
                return record;
            }
        }.transform(input.read(source));
    }
    
    @Override
    public Iterable<JSONObject> getBinding(JSONObject parameters)
    {
        var api = (tools != null) ? "chat" : "generate";
        var message = constructMessage(parameters);
        var source = new OllamaSource(api, message);
        var input = new JSONInput();
        var cursor = input.read(source);
        
        return new IdentityTransformer() {
            private Iterator<JSONObject> replacement;

            @Override
            public Iterator<JSONObject> transform(Iterator<JSONObject> iterator) throws ConvirganceException
            {
                var original = IdentityTransformer.super.transform(iterator);
                
                return new Iterator<JSONObject>() {
                    @Override
                    public boolean hasNext()
                    {
                        var iterator = (replacement != null) ? replacement : original;
                        
                        return iterator.hasNext();
                    }

                    @Override
                    public JSONObject next()
                    {
                        var iterator = (replacement != null) ? replacement : original;
                        
                        return iterator.next();
                    }
                };
            }
            
            @Override
            public JSONObject transform(JSONObject record) throws ConvirganceException
            {
                for(Advisor advisor : advisors) advisor.after(parameters, record);
                
                if(record.containsKey("message") && record.getJSONObject("message").containsKey("tool_calls"))
                {
                    replacement = getBinding(parameters, record.getJSONObject("message").getJSONArray("tool_calls")).iterator();
                    
                    return replacement.next();
                }
                
                return record;
            }
        }.transform(cursor);
    }
    
    private class OllamaSource implements Source
    {
        private String api;
        private JSONObject message;

        public OllamaSource(String api, JSONObject message)
        {
            this.api = api;
            this.message = message;
        }
        
        private HttpURLConnection getConnection()
        {
            var url = getUrl(api);
            
            try
            {
                return (HttpURLConnection)url.openConnection();
            }
            catch(IOException e) { throw new ConvirganceException(e); }
        }
        
        private byte[] getData()
        {
            try
            {
                return message.toString().getBytes("UTF-8");
            }
            catch(UnsupportedEncodingException e) { throw new ConvirganceException(e); }
        }
        
        private void handleError(InputStream in)
        {
            if(in == null) return;
            
            var out = new ByteArrayOutputStream();
            var data = new byte[4096];
            
            String result;
            int count;
            
            try
            {
                while((count = in.read(data)) > 0)
                {
                    out.write(data, 0, count);
                }
                
                result = new String(out.toByteArray(), "UTF-8");
                
                System.out.println(result);
                
                throw new ConvirganceException("Error accessing " + api + ":\n" + result);
            }
            catch(IOException e) { throw new ConvirganceException(e); }
        }
        
        @Override
        public InputStream getInputStream()
        {
            var connection = getConnection();
            var data = getData();
            
            try
            {
                connection.setRequestMethod("POST");
                connection.setDoOutput(true);
                connection.setDoInput(true);
                connection.setRequestProperty("Content-Type", "application/json"); 
                
                try(var out = connection.getOutputStream())
                {
                    out.write(data, 0, data.length);
                }
                
                handleError(connection.getErrorStream());
                
                return connection.getInputStream();
            }
            catch(IOException e) { throw new ConvirganceException(e); }
        }
        
    }
}
