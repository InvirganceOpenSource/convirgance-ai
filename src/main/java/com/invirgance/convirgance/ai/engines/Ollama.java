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

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.input.JSONInput;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.source.Source;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.List;

/**
 *
 * @author jbanes
 */
public class Ollama
{
    private String baseUrl = "http://localhost:11434/api";

    public Ollama()
    {
    }
    
    public Ollama(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
    
    public String getBaseUrl()
    {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl)
    {
        this.baseUrl = baseUrl;
    }
    
    public Iterable<JSONObject> generate(JSONObject message)
    {
        var source = new OllamaSource("generate", message);
        var input = new JSONInput();
        
        return input.read(source);
    }
    
    public Iterable<JSONObject> generate(String model, String prompt, boolean stream)
    {
        return generate(model, prompt, stream, null, null, null, null, null, false);
    }
    
    public Iterable<JSONObject> generate(String model, String prompt, boolean stream, JSONArray context)
    {
        return generate(model, prompt, stream, null, context, null, null, null, false);
    }
    
    public Iterable<JSONObject> generate(String model, String prompt, boolean stream, String suffix, JSONArray context, JSONObject options, String system, String template, boolean raw)
    {
        var message = new JSONObject();
        
        message.put("model", model);
        message.put("prompt", prompt);
        message.put("stream", stream);
        
        if(raw) message.put("raw", raw);
        
        if(context != null) message.put("context", context);
        if(suffix != null) message.put("suffix", suffix);
        if(options != null) message.put("options", options);
        if(system != null) message.put("system", system);
        if(template != null) message.put("template", template);
        
        return generate(message);
    }
    
    public Iterable<JSONObject> chat(JSONObject message)
    {
        var source = new OllamaSource("chat", message);
        var input = new JSONInput();
        
        return input.read(source);
    }
    
    public Iterable<JSONObject> chat(String model, JSONArray messages, boolean stream)
    {
        return chat(model, messages, stream, null, null);
    }
    
    public Iterable<JSONObject> chat(String model, JSONArray messages, boolean stream, JSONArray tools)
    {
        return chat(model, messages, stream, tools, null);
    }
    
    public Iterable<JSONObject> chat(String model, JSONArray messages, boolean stream, JSONArray tools, JSONObject options)
    {
        var message = new JSONObject();
        
        message.put("model", model);
        message.put("messages", messages);
        message.put("stream", stream);
        
        if(tools != null) message.put("tools", tools);
        if(options != null) message.put("options", options);
        
        return chat(message);
    }
    
    public JSONObject constructMessage(String content, Role role)
    {
        var message = new JSONObject();
        
        message.put("content", content);
        message.put("role", role.name());
        
        return message;
    }
    
    public JSONArray getLoadedModels()
    {
        var source = new OllamaSource("ps");
        var input = new JSONInput();
        var results = input.read(source);
        
        return results.iterator().next().getJSONArray("models");
    }
    
    public JSONArray getInstalledModels()
    {
        var source = new OllamaSource("tags");
        var input = new JSONInput();
        var results = input.read(source);
        
        return results.iterator().next().getJSONArray("models");
    }
    
    public JSONObject getModelDetails(String name)
    {
        var message = new JSONObject();
        var source = new OllamaSource("show", message);
        var input = new JSONInput();
        
        message.put("model", name);
        
        return input.read(source).iterator().next();
    }
    
    public Iterable<JSONObject> pullModel(String name)
    {
        return pullModel(name, false, false);
    }
    
    public Iterable<JSONObject> pullModel(String name, boolean stream, boolean insecure)
    {
        var message = new JSONObject();
        var source = new OllamaSource("pull", message);
        var input = new JSONInput();
        
        message.put("model", name);
        message.put("stream", stream);
        message.put("insecure", insecure);
        
        return input.read(source);
    }
    
    public boolean deleteModel(String name)
    {
        var message = new JSONObject();
        var source = new OllamaSource("delete", "DELETE", message);
        var input = new JSONInput();
        
        message.put("model", name);
        
        try(var iterator = input.read(source).iterator())
        {
            if(!iterator.hasNext()) return true;

            System.err.println(iterator.next());

            return false;
        }
        catch(Exception e)
        {
            return false;
        }
    }
    
    public JSONArray<JSONArray<Double>> getEmbed(String model, String input)
    {
        return getEmbed(model, new String[]{input});
    }
    
    public JSONArray<JSONArray<Double>> getEmbed(String model, String... input)
    {
        var inputs = new JSONArray(List.of(input));
        var message = new JSONObject();
        var source = new OllamaSource("embed", message);
        var json = new JSONInput();
        
        message.put("model", model);
        message.put("input", inputs);
        
        try(var iterator = json.read(source).iterator())
        {
            return iterator.next().getJSONArray("embeddings");
        }
        catch(Exception e)
        {
            throw new ConvirganceException(e);
        }
    }
    
    public static enum Role
    {
        system,
        user, 
        assistant, 
        tool
    }
    
    private class OllamaSource implements Source
    {
        private String api;
        private String method;
        private JSONObject message;

        public OllamaSource(String api)
        {
            this(api, "GET");
        }
        
        public OllamaSource(String api, JSONObject message)
        {
            this(api, "POST", message);
        }
        
        public OllamaSource(String api, String method)
        {
            this(api, method, null);
        }
        
        public OllamaSource(String api, String method, JSONObject message)
        {
            this.api = api;
            this.method = method;
            this.message = message;
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
            if(message == null) return null;
            
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
                connection.setRequestMethod(method);
                connection.setDoOutput(true);
                connection.setDoInput(true);
                
                if(message != null)
                {
                    connection.setRequestProperty("Content-Type", "application/json"); 

                    try(var out = connection.getOutputStream())
                    {
                        out.write(data, 0, data.length);
                    }
                }
                
                return connection.getInputStream();
            }
            catch(IOException e) 
            { 
                handleError(connection.getErrorStream());
                
                throw new ConvirganceException(e); 
            }
        }
        
    }
}
