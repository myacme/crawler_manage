package com.bonc.dx.crawler_manage.pool.ip;

public class Test {

    public static void main(String[] args) throws Exception {
        Thread thread1 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().getZip("");
            }
        });
        Thread thread2 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().getZip("");
            }
        });
        Thread thread3 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().getZip("");
            }
        });
        Thread thread4 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().getZip("");
            }
        });
        Thread thread5 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().getZip("");
            }
        });



        Thread thread21 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().updateZipFile("C:\\Users\\xiong\\Desktop\\proxy\\proxy0.zip",
                        "C:\\Users\\xiong\\Desktop\\proxy\\proxy11.zip",
                        "49.76.142.09:5021");
            }
        });
        Thread thread22 = new Thread(new Runnable() {
            @Override
            public void run() {
                new ProxyZipUtil().updateZipFile("C:\\Users\\xiong\\Desktop\\proxy\\proxy0.zip",
                        "C:\\Users\\xiong\\Desktop\\proxy\\proxy11.zip",
                        "49.76.142.09:5021");
            }
        });

        thread1.start();
//       Thread.sleep(500);
        thread2.start();
//        thread3.start();
        thread21.start();
        Thread.sleep(500);

        thread4.start();
        thread22.start();

        thread5.start();

    }
}
