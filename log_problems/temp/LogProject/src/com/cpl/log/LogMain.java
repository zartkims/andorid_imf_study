package com.cpl.log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class LogMain {
	public static int sWhollyPrint = 50;//如果比这个多全都打出来
	public static int sBigThan = 0;
	public static void main(String[] args) throws Exception {
//		System.out.println(new File("log_01.log").exists());
		try {
			//传递的参数第一个是检测大于多少则输出  第二个参数则是大于多少则输出全部log默认50
			
			if (null != args && args.length > 0) {
				sBigThan = getInt(args[0], 0);
				if (args.length > 1) {
					sWhollyPrint = getInt(args[1], 50);
				}
			}
			
			File file = new File("log_01.log");
			File file2 = new File("log_02.log");
			File outFile = new File("result1"+"_morethan_"+sBigThan);
			File outFile2 = new File("result2"+"_morethan_"+sBigThan);
			
			
			BufferedReader reader = new BufferedReader(new FileReader(file));
			BufferedWriter writer = new BufferedWriter(new FileWriter(outFile));
			
			dealFile(reader, writer);
			
			reader = new BufferedReader(new FileReader(file2));
			writer = new BufferedWriter(new FileWriter(outFile2));
			dealFile(reader, writer);
	        System.out.println("finish");
		} catch (Exception e) {
			System.err.print("请确保jar和log文件在同一目录下,并且第一个log文件命名为log_01.log,第二个命名为log_02.log");
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

	public static void dealFile(BufferedReader reader, BufferedWriter writer) throws Exception {
		try {
			String line;
			int lineNum = 0;
			boolean findFirstUs = false;//我们的栈的第一个log也就是直接调用那个log
			boolean whollyPrint = false;
			while ((line = reader.readLine()) != null) {
				if (line.contains("StrictMode policy violation; ~duration=")) {
					whollyPrint = false;
					int numin = line.indexOf('=') + 1;
					int afterP = line.indexOf("ms: android");
					String time = line.substring(numin, afterP);
					if (getInt(time, 0) > sBigThan) {
						if(getInt(time, 0) > sWhollyPrint) {
							whollyPrint = true; 
						}
						lineNum++;
						writer.write("\r\n\r\n===================\r\n");
						writer.write("cost time : " + time + "\r\n");
						writer.write(line+ "\r\n");
						line = reader.readLine();
						writer.write(line+ "\r\n");//这一行是类型信息
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
			writer.write("total : "+ lineNum + "\r\n");
		} catch (Exception e) {
			System.out.println(e);
			throw e;
		} finally {
			reader.close();
			writer.close();
		}
		
	}
}
