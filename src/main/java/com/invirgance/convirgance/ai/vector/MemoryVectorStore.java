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

import com.invirgance.convirgance.ConvirganceException;
import com.invirgance.convirgance.json.JSONArray;
import com.invirgance.convirgance.json.JSONObject;
import java.util.Comparator;

/**
 *
 * @author jbanes
 */
public class MemoryVectorStore
{
    private JSONArray<JSONObject> documents = new JSONArray<>();
    
    private double distance = 0.7;

    public MemoryVectorStore()
    {
    }

    public double getDistance()
    {
        return distance;
    }

    public void setDistance(double distance)
    {
        this.distance = distance;
    }
    
    public void register(JSONArray<Double> embed, String document)
    {
        var record = new JSONObject();
        
        record.put("embed", embed);
        record.put("document", document);
        
        this.documents.add(record);
    }
    
    public String match(JSONArray<Double> embed)
    {
        var matches = matches(embed);
        
        if(!matches.isEmpty()) return matches.get(0).getString("document");
        
        return null;
    }
    
    public JSONArray<JSONObject> matches(JSONArray<Double> embed)
    {
        var matches = new JSONArray<JSONObject>();
        var match = new JSONObject();
        var distance = 0.0;
        
        for(var record : this.documents)
        {
            distance = 1.0 - computeCosineSimilarity(embed, record.getJSONArray("embed"));

            if(distance <= this.distance)
            {
                match = new JSONObject();
                
                match.put("distance", distance);
                match.put("document", record.get("document"));
                matches.add(match);
            }
        }
        
        matches.sort((JSONObject left, JSONObject right) -> {
            double leftDistance = left.getDouble("distance");
            double rightDistance = right.getDouble("distance");
            double difference = leftDistance - rightDistance;
            
            if(difference < 0) return -1;
            if(difference > 0) return 1;
            
            return 0;
        });
        
        return matches;
    }
    
    public static double computeDotProduct(JSONArray<Double> a, JSONArray<Double> b)
    {
        var products = new double[a.size()];
        var sum = 0.0;
        
        if(a.size() != b.size()) throw new ConvirganceException("Vector size mismatches: " + a.size() + " != " + b.size());
        
        for(int i=0; i<a.size(); i++)
        {
            products[i] = a.getDouble(i) * b.getDouble(i);
            sum += products[i];
        }
        
        return sum;
    }
    
    public static double computeMagnitude(JSONArray<Double> vector)
    {
        var squared = new double[vector.size()];
        var sum = 0.0;
        
        for(int i=0; i<vector.size(); i++)
        {
            squared[i] = Math.pow(vector.getDouble(i), 2);
            sum += squared[i];
        }
        
        return Math.sqrt(sum);
    }
    
    public static double computeCosineSimilarity(JSONArray<Double> a, JSONArray<Double> b)
    {
        var product = computeDotProduct(a, b);
        var magnitudes = new double[]{ computeMagnitude(a), computeMagnitude(b) };
        
        return (product / (magnitudes[0] * magnitudes[1]));
    }
}
