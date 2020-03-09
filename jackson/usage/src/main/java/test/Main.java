package test;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import test.generic.ContainsList;
import test.generic.GenericHolder;
import test.some.Test2;
import test.some.Test3;
import test.some.Test4;
import test.some.Test5;
import test.some.Test7;
import test.some.Test8;
import test.some.TestCircle;
import test.some.TestRectangle;

public class Main {
    public static void main(String[] args) {
        try {
            final JsonFactory jsonFactory = JsonFactory.builder()
                    .build();
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            Test test = new Test("my secret", "my final");
            test.another = "my another";
            test.first = "my first";
            test.namedProperty = "NaMeD";
            test.inner = new Test2(-21, "inner", 42);
            test.dank = new Test4(new Test3(new Test4(null)));
            test.list = new Test5(new Test5(new Test5(null, 3), 2), 1);
            final ContainsList containsList = new ContainsList();
            test.containsList = containsList;
            containsList.setList(new ArrayList<>(Arrays.asList("aba", "caba", "baba")));
            containsList.magic = 42;
            test.stringHolder = new GenericHolder<>("aaa");
            final ArrayList<GenericHolder<Test2>> genericHolders = new ArrayList<>();
            genericHolders.add(new GenericHolder<>(null));
            genericHolders.add(new GenericHolder<>(new Test2(1, "one", -1)));
            genericHolders.add(new GenericHolder<>(new Test2(2, "two", -3)));
            test.hell = new GenericHolder<>(genericHolders);
            test.tripleGeneric = new GenericHolder<>(new GenericHolder<>(new GenericHolder<>("wut")));
            test.tripleList = new ArrayList<>(2);
            test.tripleList.add(new ArrayList<>(1));
            test.tripleList.add(new ArrayList<>(2));
            test.tripleList.get(0).add(new ArrayList<>(1));
            test.tripleList.get(1).add(new ArrayList<>(2));
            test.tripleList.get(1).add(new ArrayList<>(3));
            test.tripleList.get(0).get(0).add("qwerty");
            test.tripleList.get(1).get(0).add("asdf");
            test.tripleList.get(1).get(0).add("movie");
            test.tripleList.get(1).get(1).add("az");
            test.tripleList.get(1).get(1).add("Az");
            test.tripleList.get(1).get(1).add("aZ");
            test.blinking = new ArrayList<>(4);
            test.blinking.add(new Test8(1));
            test.blinking.add(new Test7("2"));
            test.blinking.add(new Test8(3));
            test.blinking.add(new Test7("4"));
            test.interfaced = "yup";
            test.cEnumPaper = 1;
            test.day = "Saturday";
            test.setName("Alex", "Darkstalker");
            test.shapes = new ArrayList<>(Arrays.asList(new TestCircle(1.0), new TestRectangle(0.5, 2.0), new TestRectangle(0.25, 0.125), new TestCircle(5.0)));
            System.out.println("toString():");
            System.out.println(test.toString());
            System.out.println("--- end of object ---");
            TestAdapter adapter = TestAdapter.Holder.INSTANCE;
            final JsonGenerator generator = jsonFactory.createGenerator(out);
            generator.useDefaultPrettyPrinter();
            adapter.write(test, generator);
            generator.flush();
            out.close();
            byte[] arr = out.toByteArray();
            System.out.println("serialized:");
            System.out.println(new String(arr));
            System.out.println("--- end of object ---");
            ByteArrayInputStream in = new ByteArrayInputStream(arr);
            final JsonParser parser = jsonFactory.createParser(in);
            parser.nextToken();
            Test got = adapter.toObject(parser);
            System.out.println("parsed:");
            System.out.println(got.toString());
            System.out.println("--- end of object ---");
            System.out.println("equals? " + test.equals(got));
        } catch (Throwable t) {
            t.printStackTrace();
        }
    }
}
