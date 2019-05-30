package ru.geekbrains;

public class Task2 {

    public int[] doIt (int[] arr){
        int position = -1;
        if (arr.length >= 3 && arr[arr.length-1] !=4){
            for (int i = 0; i < arr.length; i++) {
                if (arr[i] == 4){
                    position = i;
                }
            }
            if (position == -1) throw new RuntimeException();

            int[] result = new int[arr.length - position - 1];

            System.arraycopy(arr, position + 1, result, 0, result.length);
            return result;

        }
        return null;
    }
}
