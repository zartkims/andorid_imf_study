package com.sohu.cpl.boradcast_log;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

public class BroadcastLogMain {
	public static final String UNKNOWN = "unknown";
	public static final String NUM = "-n";
	public static final String BROADCAST_ONLY = "-o";
	public static final String BROADCAST_EXCLUDE = "-e";
	
	public static int sCostThreshold = 20;
	public static String sUnCountBroadcast = UNKNOWN;
	public static String sOnlyBroadcast = UNKNOWN;
	
	
	public static void main(String[] args) {
		sUnCountBroadcast = UNKNOWN;
		sOnlyBroadcast = UNKNOWN;
		if (args != null) {
			int len = args.length;
			if (len % 2 != 0) len = len - 1;//
			for (int i = 0; i < len; ) {
				String par = args[i];
				String value = args[i+1];
				setPar(par, value);
				i += 2;
			}
		}
		File logFile = new File("broadcastFile");
		try {
			String exlude =  UNKNOWN.equals(sUnCountBroadcast)  ?  "" : ("_exclud_" + sUnCountBroadcast);
			String only =  UNKNOWN.equals(sOnlyBroadcast) ? "":("_only_" + sOnlyBroadcast);
			String outputFileName = "costmorethan_" + sCostThreshold  + exlude + only  ;
			dealLogFile(logFile, outputFileName);
		} catch (Exception e) {
			e.printStackTrace();
			System.err.println("any problem occur, ask caipengli" + e.toString());
		}
		System.out.println("finish");
	}
	
	private static void setPar(String par, String value) {
		if (NUM.equals(par)) {
			sCostThreshold = getInt(value, 20);
		} else if (BROADCAST_ONLY.equals(par)) {
			sOnlyBroadcast = value;
		} else if (BROADCAST_EXCLUDE.equals(par)) {
			sUnCountBroadcast = value;
		}
	}

	public static void dealLogFile(File file, String outName) throws Exception {
		FileReader fReader = null;
		FileWriter fWriter = null;
		BufferedReader bReader = null;
		BufferedWriter bWriter = null;
		int totalInfo = 0;
		int validInfo = 0;
		try {
			fReader = new FileReader(file);
			fWriter  = new FileWriter(new File(outName));
			bReader = new BufferedReader(fReader);
			bWriter = new BufferedWriter(fWriter);
			String line;
			while (null != (line = bReader.readLine())) {
				if (line.trim().equals("")||line.endsWith("****")) {//it is record log  was wrote time
					continue;
				}
				String processName = line;
				String broadcastName = bReader.readLine();
				String stactInfo = bReader.readLine();
				int costTime = getInt(bReader.readLine(), -1);
				String occurTime = bReader.readLine();
				String separ = bReader.readLine();
				totalInfo++;
//				System.out.println(processName +" " +broadcastName + " " + stactInfo +" " + costTime + " " + occurTime + " " + separ);
				if (costTime >= sCostThreshold &&  !sUnCountBroadcast.equals(broadcastName)) {
					if ((sOnlyBroadcast.equals(UNKNOWN) || sOnlyBroadcast.equals(broadcastName))) {
						validInfo++;
						bWriter.write(separ + "\r\n");
						bWriter.write(costTime + "\r\n");
						bWriter.write(broadcastName + "\r\n");
						bWriter.write(processName + "\r\n");
						bWriter.write(stactInfo + "\r\n");
						bWriter.write(occurTime + "\r\n");
					}
					
				}
			}
			
			bWriter.write("***********************\r\n");
			float percent = 1;
			if (totalInfo != 0) percent = validInfo * 100f / totalInfo ;
			bWriter.write("valid info num : " + validInfo + "   total info num : " + totalInfo + "  percent : " + percent + "%");
		} catch (FileNotFoundException e) {
			System.err.println(e.toString());
		} catch (Exception e) {
			System.err.println(e.toString());
			throw e;
		} finally {
			closeStream(bWriter);
			closeStream(fWriter);
			closeStream(bReader);
			closeStream(fReader);
		}
	}
	
	private static int getInt(String str, int def) {
		try {
			return Integer.valueOf(str);
		} catch(Exception e) {
			return def;
		}
	}

	public static void closeStream(Closeable stream) {
		if (null == stream) return;
		try {
			stream.close();
		} catch (IOException e) {
			System.err.println(e.toString());
		}
	}
}
