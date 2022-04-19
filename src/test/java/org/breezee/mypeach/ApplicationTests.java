package org.breezee.mypeach;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

@SpringBootTest
class ApplicationTests {

    @Test
    void contextLoads() {

    }

    @Test
    void test() {
        String sOne = "TO_DATE('#TFLG#','yyyy-MM-dd') ) )";
        List<String> strings = Arrays.asList(sOne.split(""));
        System.out.println(strings.get(0));
        System.out.println(strings.get(1));
        //strings.stream().filter(t-> System.out.println(t));
        long leftCount =0;
        //long leftCount = strings.stream().filter(t-> System.out.println(t)).count();
        //long rightCount = Stream.of(sOne.toCharArray()).filter(t->t.equals(')')).count();

//        for (Character ch:sOne.toCharArray()) {
//            System.out.println(ch);
//        }
        System.out.println(leftCount);
        //System.out.println(rightCount);
    }

}
