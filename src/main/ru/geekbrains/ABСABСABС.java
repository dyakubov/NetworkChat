package ru.geekbrains;

public class ABСABСABС {
    private static volatile char currentLetter = 'A';

    public static void main(String[] args) {
        new Thread(ABСABСABС::printA).start();
        new Thread(ABСABСABС::printB).start();
        new Thread(ABСABСABС::printC).start();
    }

    private synchronized static void printA() {
        for (int i = 0; i < 5; i++) {
            try {
                while (currentLetter != 'A') {
                    ABСABСABС.class.wait();
                }
                Thread.sleep(100);
                System.out.println("A");
                currentLetter = 'B';
                ABСABСABС.class.notifyAll();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private synchronized static void printB() {
        for (int i = 0; i < 5; i++) {
            try {
                while (currentLetter != 'B') {
                    ABСABСABС.class.wait();
                }
                Thread.sleep(100);
                System.out.println("B");
                currentLetter = 'С';
                ABСABСABС.class.notifyAll();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }

    private synchronized static void printC() {
        for (int i = 0; i < 5; i++) {
            try {
                while (currentLetter != 'С') {
                    ABСABСABС.class.wait();
                }
                Thread.sleep(100);
                System.out.println("C");
                currentLetter = 'A';
                ABСABСABС.class.notifyAll();
            } catch (InterruptedException ex) {
                ex.printStackTrace();
            }
        }
    }
}
