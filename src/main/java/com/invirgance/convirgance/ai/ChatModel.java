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

import com.invirgance.convirgance.json.JSONObject;
import com.invirgance.convirgance.web.binding.Binding;

/**
 *
 * @author jbanes
 */
public interface ChatModel extends Binding
{
    public default String template(String template, JSONObject parameters)
    {
        var buffer = new StringBuffer(template.length());
        var name = new StringBuffer();
        char c;
        
        for(int i = 0; i<template.length(); i++)
        {
            c = template.charAt(i);
            
            if(c == '\\' && i<template.length()-1 && template.charAt(i+1) == '$')
            {
                buffer.append('$');
                i++;
            }
            else if(c == '$' && i<template.length()-1 && template.charAt(i+1) == '{')
            {
                i += 2;
                name.setLength(0);
                
                while(i < template.length() && (c = template.charAt(i++)) != '}')
                {
                    name.append(c);
                }
                
                buffer.append(String.valueOf(parameters.get(name.toString())));
            }
            else
            {
                buffer.append(c);
            }
        }
        
        return buffer.toString();
    }
}
