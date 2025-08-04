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
package com.invirgance.convirgance.ai.tools;

import com.invirgance.convirgance.ai.annotations.Tool;
import com.invirgance.convirgance.ai.annotations.ToolParam;
import com.invirgance.convirgance.wiring.annotation.Wiring;

/**
 *
 * @author jbanes
 */
@Wiring
public class ComputeTool
{
    @Tool("Adds two numbers together")
    public double add(
            @ToolParam("First number to add together") double left, 
            @ToolParam("Second number to add together") double right)
    {
        return left + right;
    }
    
    @Tool("Subtracts the second number from the first number")
    public double subtract(
            @ToolParam("Starting number to subtract from") double left, 
            @ToolParam("Value to subtract from the other number") double right)
    {
        return left - right;
    }
    
    @Tool("Multiplies two numbers together")
    public double multiply(
            @ToolParam("First number to multiply") double left, 
            @ToolParam("Second number to multiply") double right)
    {
        return left * right;
    }
    
    @Tool("Divides the first number by the second number")
    public double divide(
            @ToolParam("The dividend to be divided") double dividend,
            @ToolParam("The divisor to divide with") double divisor)
    {
        return dividend / divisor;
    }
}
