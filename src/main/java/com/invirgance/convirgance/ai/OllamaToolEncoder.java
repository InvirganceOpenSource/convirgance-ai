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
import com.invirgance.convirgance.ai.annotations.Tool;
import com.invirgance.convirgance.ai.annotations.ToolParam;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 * @author jbanes
 */
public class OllamaToolEncoder
{
    private List tools;
    private Map<String,Method> methods = new HashMap<>();
    private Map<String,Object> instances = new HashMap<>();
    
    private JSONArray descriptors;

    public OllamaToolEncoder(Object... tools)
    {
        this.tools = List.of(tools);
        this.descriptors = getDescriptors(tools);
    }
    
    private Object parseArray(Class type, List values)
    {
        Class arrayType = type.componentType();
        Object array = Array.newInstance(arrayType, values.size());
        
        for(int i=0; i<values.size(); i++)
        {
            if(arrayType.equals(boolean.class)) Array.setBoolean(array, i, (Boolean)values.get(i));
            else if(arrayType.equals(byte.class)) Array.setByte(array, i, (Byte)values.get(i));
            else if(arrayType.equals(char.class)) Array.setChar(array, i, (Character)values.get(i));
            else if(arrayType.equals(double.class)) Array.setDouble(array, i, (Double)values.get(i));
            else if(arrayType.equals(float.class)) Array.setFloat(array, i, (Float)values.get(i));
            else if(arrayType.equals(int.class)) Array.setInt(array, i, (Integer)values.get(i));
            else if(arrayType.equals(long.class)) Array.setLong(array, i, (Long)values.get(i));
            else if(arrayType.equals(short.class)) Array.setShort(array, i, (Short)values.get(i));
            else Array.set(array, i, values.get(i));
        }
        
        return array;
    }
    
    private String[] parseStringArray(String value)
    {
        String[] values = value.split(",");
        
        for(int i=0; i<values.length; i++) values[i] = values[i].trim();
        
        return values;
    }
    
    private Object coerceValue(Class type, Object value)
    {
        if(type.equals(JSONObject.class) && !(value instanceof JSONObject))
        {
            if(value instanceof Map) return new JSONObject((Map)value);
            else return new JSONObject(value.toString());
        }
        
        if(type.equals(JSONArray.class) && !(value instanceof JSONArray))
        {
            if(value instanceof Collection) return new JSONArray((Collection)value);
            else return new JSONArray(value.toString());
        }
        
        if(type.isArray() && value instanceof List) 
        {
            return parseArray(type, (List)value);
        }
            
        if(!(value instanceof String)) return value;

        if(type.isPrimitive())
        {
            if(type.equals(byte.class)) return Byte.valueOf((String)value);
            if(type.equals(short.class)) return Short.valueOf((String)value);
            if(type.equals(int.class)) return Integer.valueOf((String)value);
            if(type.equals(long.class)) return Long.valueOf((String)value);
            if(type.equals(boolean.class)) return Boolean.valueOf((String)value);
            if(type.equals(float.class)) return Float.valueOf((String)value);
            if(type.equals(double.class)) return Double.valueOf((String)value);
            if(type.equals(char.class)) return value.toString().charAt(0);
        }
        else
        {
            if(type.equals(Byte.class)) return Byte.valueOf((String)value);
            if(type.equals(Short.class)) return Short.valueOf((String)value);
            if(type.equals(Integer.class)) return Integer.valueOf((String)value);
            if(type.equals(Long.class)) return Long.valueOf((String)value);
            if(type.equals(Boolean.class)) return Boolean.valueOf((String)value);
            if(type.equals(Float.class)) return Float.valueOf((String)value);
            if(type.equals(Double.class)) return Double.valueOf((String)value);
            if(type.equals(Character.class)) return value.toString().charAt(0);
            if(type.equals(String[].class)) return parseStringArray((String)value);
        }
        
        return value;
    }

    public JSONArray getDescriptors()
    {
        return descriptors;
    }
    
    public String execute(JSONObject toolCall)
    {
        var function = toolCall.getJSONObject("function").getString("name");
        var arguments = toolCall.getJSONObject("function").getJSONObject("arguments");
        var argument = "";
        
        var method = this.methods.get(function);
        var instance = this.instances.get(function);
        
        var parameters = new Object[method.getParameterCount()];
        var index = 0;
        
        for(var parameter : method.getParameters())
        {
            argument = arguments.getString(parameter.getName());
            parameters[index++] = coerceValue(parameter.getType(), argument);
        }
        
        try
        {
            return String.valueOf(method.invoke(instance, parameters));
        }
        catch(ReflectiveOperationException e)
        {
            throw new ConvirganceException(e);
        }
    }
    
    public JSONObject getDescriptor(Method method, Object instance)
    {
        var descriptor = new JSONObject();
        var function = new JSONObject();
        var parameters = new JSONObject();
        var properties = new JSONObject();
        var required = new JSONArray();
        
        Tool tool = method.getAnnotation(Tool.class);
        ToolParam toolParam;
        JSONObject property;
        String type;
        
        if(methods.containsKey(method.getName()))
        {
            throw new ConvirganceException("Function " + method.getName() + " being overridden by " + instance.getClass() + " is already registered by " + instances.get(method.getName()).getClass());
        }
        
        descriptor.put("type", "function");
        descriptor.put("function", function);
        
        function.put("name", method.getName());
        function.put("parameters", parameters);
        
        if(tool != null && tool.value().length() > 0) 
        {
            function.put("description", tool.value());
        }
        
        parameters.put("type", "object");
        parameters.put("properties", properties);
        parameters.put("required", required);
        
        for(var parameter : method.getParameters())
        {
            property = new JSONObject();
            
            required.add(parameter.getName());
            
            if(parameter.getType().equals(String.class)) type = "string";
            else if(parameter.getType().isPrimitive()) type = "number";
            else throw new ConvirganceException("Unrecognized parameter type " + parameter.getType() + " for parameter " + parameter.getName());
            
            toolParam = parameter.getAnnotation(ToolParam.class);

            if(toolParam != null && toolParam.value().length() > 0) 
            {
                property.put("description", toolParam.value());
            }
            
            property.put("type", type);
            properties.put(parameter.getName(), property);
        }
        
        this.methods.put(method.getName(), method);
        this.instances.put(method.getName(), instance);
        
        return descriptor;
    }
    
    public JSONArray getDescriptors(Object tool)
    {
        var array = new JSONArray();
        var clazz = tool.getClass();
        
        for(var method : clazz.getMethods())
        {
            if(method.getAnnotation(Tool.class) == null) continue;
            
            array.add(getDescriptor(method, tool));
        }
        
        if(array.size() < 1)
        {
            throw new ConvirganceException(clazz.getName() + " does not contain any tools");
        }
        
        return array;
    }
    
    
    public JSONArray getDescriptors(Object... tools)
    {
        var array = new JSONArray();
        
        for(var tool : tools)
        {
            array.addAll(getDescriptors(tool));
        }
        
        return array;
    }
}
