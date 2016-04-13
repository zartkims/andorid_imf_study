package com.cpl.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LogMain {
	public static int sWhollyPrint = 50;//����������ȫ�������
	public static void main(String[] args) throws Exception {
//		System.out.println(new File("log_01.log").exists());
		try {
			//���ݵĲ�����һ���Ǽ����ڶ��������  �ڶ����������Ǵ��ڶ��������ȫ��logĬ��50
			int bigThan = 0;
			if (null != args && args.length > 0) {
				bigThan = getInt(args[0], 0);
				if (args.length > 1) {
					sWhollyPrint = getInt(args[1], 50);
				}
			}
			
			File file = new File("log_01.log");
			File file2 = new File("log_02.log");
			File outFile = new File("result1"+"_bigthan_"+bigThan);
			File outFile2 = new File("result2"+"_bigthan_"+bigThan);
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			
			dealFile(reader, writer, bigThan);
			
			reader = new BufferedReader(new FileReader(file2));
			writer = new BufferedWriter(new FileWriter(outFile2));
			dealFile(reader, writer, bigThan);
	        System.out.println("finish");
		} catch (Exception e) {
			System.err.print("��ȷ��jar��log�ļ���ͬһĿ¼��,���ҵ�һ��log�ļ�����Ϊlog_01.log,�ڶ�������Ϊlog_02.log");
			System.out.println(e);
		}
		
	}
	
	private static Integer getInt(String num, int def) {
		try {
			int res = Integer.valueOf(num.trim());
			return res;
		} catch (Exception e) {
			return def;
		}
	}

	public static void dealFile(BufferedReader reader, BufferedWriter writer, int big) throws Exception {
		try {
			String line;
			boolean findFirstUs = false;//���ǵ�ջ�ĵ�һ��logҲ����ֱ�ӵ����Ǹ�log
			boolean whollyPrint = false;
			while ((line = reader.readLine()) != null) {
				if (line.contains("StrictMode policy violation; ~duration=")) {
					whollyPrint = false;
					int numin = line.indexOf('=') + 1;
					int afterP = line.indexOf("ms: android");
					String time = line.substring(numin, afterP);
					if (getInt(time, 0) > big) {
						if(getInt(time, 0) > sWhollyPrint) {
							whollyPrint = true; 
						}
						writer.write("\r\n\r\n===================\r\n");
						writer.write("cost time : " + time + "\r\n");
						writer.write(line+ "\r\n");
						line = reader.readLine();
						writer.write(line+ "\r\n");//��һ����������Ϣ
						findFirstUs = true;
					}
				}
				if (whollyPrint) {
					findFirstUs = false;
					writer.write(line+"\r\n");
				} else if (findFirstUs) {
					if (line.contains("at com.sogou.")) {
						findFirstUs = false;
						writer.write(line+"\r\n");
					}
				}
			}
		} catch (Exception e) {
			System.out.println(e);
			throw e;
		} finally {
			reader.close();
			writer.close();
		}
		
	}
}
