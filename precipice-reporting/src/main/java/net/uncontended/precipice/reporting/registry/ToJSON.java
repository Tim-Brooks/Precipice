/*
 * Copyright 2016 Timothy Brooks
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.uncontended.precipice.reporting.registry;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import net.uncontended.precipice.Failable;

import java.io.IOException;
import java.io.StringWriter;

public class ToJSON<Result extends Enum<Result> & Failable, Rejected extends Enum<Rejected>> {


    private static final String EMPTY = "{}";
    private JsonFactory jsonFactory = new JsonFactory();

    public String write(Summary<Result, Rejected> summary) {
        Slice<Result, Rejected>[] slices = summary.getSlices();

        Class<Result> resultClazz = summary.resultClazz;
        Class<Rejected> rejectedClazz = summary.rejectedClazz;

        StringWriter w = new StringWriter();
        try {
            JsonGenerator generator = jsonFactory.createGenerator(w);
            generator.writeStartObject();
            generator.writeObjectFieldStart("result-to-success?");
            for (Result r : resultClazz.getEnumConstants()) {
                generator.writeObjectField(r.toString(), r.isFailure());
            }
            generator.writeEndObject();
            generator.writeArrayFieldStart("rejected");
            for (Rejected r : rejectedClazz.getEnumConstants()) {
                generator.writeString(r.toString());
            }
            generator.writeEndArray();
            generator.writeArrayFieldStart("slices");
            writeSlice(generator, slices[0]);
            generator.writeEndArray();
            generator.writeEndObject();
            generator.flush();
        } catch (IOException e) {
            e.printStackTrace();
            return EMPTY;
        }

        return w.toString();
    }

    private void writeSlice(JsonGenerator generator, Slice<Result, Rejected> slice) throws IOException {
        generator.writeStartObject();
        generator.writeObjectField("start-epoch", slice.startEpoch);
        generator.writeObjectField("end-epoch", slice.endEpoch);

        generator.writeObjectFieldStart("total-result-counts");
        for (Result result : slice.resultClazz.getEnumConstants()) {
            generator.writeObjectField(result.toString(), slice.totalResultCounts[result.ordinal()]);
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart("result-counts");
        for (Result result : slice.resultClazz.getEnumConstants()) {
            generator.writeObjectField(result.toString(), slice.resultCounts[result.ordinal()]);
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart("total-rejected-counts");
        for (Rejected result : slice.rejectedClazz.getEnumConstants()) {
            generator.writeObjectField(result.toString(), slice.totalRejectedCounts[result.ordinal()]);
        }
        generator.writeEndObject();

        generator.writeObjectFieldStart("rejected-counts");
        for (Rejected result : slice.rejectedClazz.getEnumConstants()) {
            generator.writeObjectField(result.toString(), slice.rejectedCounts[result.ordinal()]);
        }
        generator.writeEndObject();

        generator.writeEndObject();
    }
}
