package org.example;

import com.alibaba.fastjson.JSON;

import java.io.*;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.CountDownLatch;

/**
 * @author Liwq
 */
public class Main {

    private static String sourceFilePath;

    private static String addrField;

    private static String targetFilePath;

    public static void main(String[] args) throws InterruptedException {
        System.out.println(" 开始执行地址经纬度查询....");

        if (args.length == 0 || isHelp(args[0])) {
            System.out.println("通过地址获取地址的经纬度工具类，请按照如下*顺序*进行输入：\n" +
                    "  -s , 指定源文件，只支持 csv，且需要表头\n" +
                    "  -f , 指定地址字段\n" +
                    "  -t , 指定输出文件，输出文件包含源文件字段，以及获取的字段"
            );
        }

        // 解析参数
        analysisParam(args);
        if (isNullOrEmpty(sourceFilePath)) {
            System.out.println("请输入源文件路径");
            return;
        }
        if (isNullOrEmpty(addrField)) {
            System.out.println("请输入地址字段");
            return;
        }
        if (isNullOrEmpty(targetFilePath)) {
            System.out.println("请指定输出文件");
            return;
        }

        List<String> tmpFileList = new ArrayList<>();
        int region = 0;
        try (BufferedReader sourceFileReader = new BufferedReader(new FileReader(sourceFilePath))) {
            long lineAmount = getTotalLines(new File(sourceFilePath));

            // 分区数量可以变更
            long regionAmount = 10000L;
            region = (int) (lineAmount % regionAmount == 0 ? lineAmount / regionAmount : lineAmount / regionAmount + 1);
            if (regionAmount > lineAmount) {
                region = 1;
            }
            if (region > 1) {
                System.out.println("数据量较大，进行数据分区，分区数：" + region);
                File sourceFile = new File(sourceFilePath);
                String pDir = sourceFile.getParent();
                String filename = sourceFile.getName();
                List<BufferedWriter> writers = new ArrayList<>();
                String header = sourceFileReader.readLine();
                for (int i = 0; i < region; i++) {
                    String tmpFile = pDir + "/" + "tmp_" + i + "_" + filename;
                    tmpFileList.add(tmpFile);
                    final BufferedWriter writer = new BufferedWriter(new FileWriter(tmpFile));
                    writer.write(header);
                    writer.newLine();
                    writers.add(writer);
                }
                String dataLine;
                long index = 0;
                while ((dataLine = sourceFileReader.readLine()) != null) {
                    index++;
                    final BufferedWriter bufferedWriter = writers.get((int) (index % region));
                    bufferedWriter.write(dataLine);
                    bufferedWriter.newLine();

                }
                for (BufferedWriter writer : writers) {
                    writer.flush();
                    writer.close();
                }
                System.out.println("分区完成!");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }


        if (region < 1) {
            extracted(sourceFilePath, targetFilePath);

        } else {
            CountDownLatch countDownLatch = new CountDownLatch(region);
            for (String tmpFilePath : tmpFileList) {
                Thread thread = new Thread(
                        () -> {
                            extracted(tmpFilePath, tmpFilePath + "t");
                            countDownLatch.countDown();
                        }
                );
                thread.start();
            }
            countDownLatch.await();
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(targetFilePath))) {
                String header = null;
                for (String s : tmpFileList) {
                    final BufferedReader reader = new BufferedReader(new FileReader(s + "t"));
                    if(header == null){
                        header = reader.readLine();
                        writer.write(header);
                        writer.newLine();
                    }else {
                        reader.readLine();
                    }
                    String line;
                    while ((line = reader.readLine()) != null) {
                        writer.write(line);
                        writer.newLine();
                    }
                    writer.flush();
                    reader.close();

                    new File(s).delete();
                    new File(s + "t").delete();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }


        System.out.println("执行完成");
    }

    private static void extracted(String sourceFilePath, String targetFilePath) {
        try (BufferedReader reader = new BufferedReader(new FileReader(sourceFilePath));
             BufferedWriter writer = new BufferedWriter(new FileWriter(targetFilePath))
        ) {

            String header = reader.readLine();
            String[] fields = header.split(",");
            int addrFieldIndex = Arrays.asList(fields).indexOf(addrField);
            if (addrFieldIndex < 0) {
                System.out.println("输入地址字段,不合法" + String.join(",", fields));
                return;
            }

            writer.write(header + ",lon,lat");
            writer.newLine();

            String addrUrl = "https://www.piliang.tech/api/amap/geocode?address=";
            String dataLine;
            while ((dataLine = reader.readLine()) != null) {
                String[] values = dataLine.split(",",-1);
                if (values.length != fields.length) {
                    continue;
                }
                String addr = values[addrFieldIndex];
                if (isNullOrEmpty(addr)) {
                    continue;
                }

                String lonlat = process(addrUrl, addr);
                writer.write(dataLine + "," + lonlat);
                writer.newLine();

            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 获取文件行数
     */
    public static int getTotalLines(File file) throws IOException {
        FileReader in = new FileReader(file);
        LineNumberReader reader = new LineNumberReader(in);
        reader.skip(Long.MAX_VALUE);
        int lines = reader.getLineNumber();
        reader.close();
        return lines;
    }

    public static String process(String addrUrl, String addr) {
        try (InputStream in = new URL(addrUrl + addr).openStream()) {
            InputStreamReader inR = new InputStreamReader(in);
            BufferedReader br = new BufferedReader(inR);
            StringBuilder resultBuffer = new StringBuilder();
            String line;
            while ((line = br.readLine()) != null) {
                resultBuffer.append(line);
            }
            ResultBean resultBean = JSON.parseObject(resultBuffer.toString(), ResultBean.class);
            // 处理数据
            if (resultBean.isSuccess()) {
                return resultBean.getGeocodes().get(0).getLocation();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return ",";
    }

    private static boolean isNullOrEmpty(String str) {
        return str == null || "".equals(str.trim());
    }

    private static void analysisParam(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if ("-s".equalsIgnoreCase(args[i].trim())) {
                sourceFilePath = args[i + 1];
            }
            if ("-f".equalsIgnoreCase(args[i].trim())) {
                addrField = args[i + 1];
            }
            if ("-t".equalsIgnoreCase(args[i].trim())) {
                targetFilePath = args[i + 1];
            }
        }
    }

    private static boolean isHelp(String arg) {
        String[] he = {"-h", "--help"};

        for (String s : he) {
            if (s.equals(arg)) {
                return true;
            }
        }
        return false;
    }

}
