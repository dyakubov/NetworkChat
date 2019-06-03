package ru.geekbrains;

import java.util.Arrays;
import java.util.List;

public class Task3 {
    public boolean doIt (Integer[] arr){
        List<Integer> arrList = Arrays.asList(arr);
        return arrList.contains(1) && arrList.contains(4);
    }
}
