package com.vetautet.app.shared.common.util;

public class SingleFlightTest {

    public static int sum(int a, int b) {
        try {
            System.out.println("start");
            Thread.sleep(1000);
            System.out.println("end");
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        return a + b;
    }

    public static void main(String[] args) {
        SingleFlight<String, Integer> sf = new SingleFlight<>();

        Runnable request = () -> {
            int result = sf.doCall("getUser_123", () ->  {
                System.out.println("👉 Đang truy vấn Database thực tế..."); // Chỉ in ra 1 lần
                return sum(1, 2);
            });
            System.out.println("Kết quả nhận được: " + result);
        };

        for (int i = 0; i < 100; i++) {
            new Thread(request).start();
        }

    }
}
 