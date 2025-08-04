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
}
