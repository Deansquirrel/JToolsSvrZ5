package com.yuansong.tools.svrz5;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class SvrZ5Helper {
	
	private SvrZ5Helper() {};
	
	private static final byte chSplit = 9;
	private static final byte chEdge = 0;
	
	private static int timeout = 10000;

	public static int getTimeout() {
		return timeout;
	}

	public static void setTimeout(int timeout) {
		SvrZ5Helper.timeout = timeout;
	}
	
	/**
	 * 获取总部账套查询语句
	 * @param flag 是否仅正式账套
	 * @return
	 */
	public static String getZBAccSql(boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append(""
				+ "select accname "
				+ "from zlaccount30 ");
		sb.append("where accisdeleted = 0 ");
			if(flag) {
				sb.append("and acctype = 1 ");
			}
			sb.append("order by accname asc");
		return sb.toString();
	}
	
	/**
	 * 获取门店总部账套查询语句
	 * @param flag 是否仅正式账套
	 * @return
	 */
	public static String getMDAccSql(boolean flag) {
		StringBuilder sb = new StringBuilder();
		sb.append(""
				+ "select accname "
				+ "from zlraccount31 ");
		sb.append("where accisdeleted = 0 ");
			if(flag) {
				sb.append("and acctype = 1 ");
			}
			sb.append("order by accname asc");
		return sb.toString();
	}
	
	public static SQLConfig getZBSQLConfig(String server) throws UnknownHostException, IOException  {
		return SvrZ5Helper.getSQLConfig(server, 7050, 30, timeout);
	}
	
	public static SQLConfig getZBSQLConfig(String server, int timeout) throws UnknownHostException, IOException  {
		return SvrZ5Helper.getSQLConfig(server, 7050, 30, timeout);
	}
	
	public static SQLConfig getMDSQLConfig(String server) throws UnknownHostException, IOException  {
		return SvrZ5Helper.getSQLConfig(server, 7051, 31, timeout);
	}
	
	public static SQLConfig getMDSQLConfig(String server, int timeout) throws UnknownHostException, IOException  {
		return SvrZ5Helper.getSQLConfig(server, 7051, 31, timeout);
	}

	private static SQLConfig getSQLConfig(String server, int port, int appType, int timeout) throws UnknownHostException, IOException  {
		String str = new String(SvrZ5Helper.getSvrSocketReturn(server, port, appType, timeout));
		str = str.replaceAll(new String(new byte[] {chEdge}), "");
		String[] list = str.split(new String(new byte[] {chSplit}));
		
		if(list.length < 3) {
			throw new RuntimeException("convert sql config error, limit 2 act " + String.valueOf(list.length));
		}
		if(!list[0].equals("RESCONNECT")) {
			throw new RuntimeException("convert sql config error, not export format");
		}
		if(list[1].equals("0")) {
			throw new RuntimeException("error: " + list[2]);
		} else if (!list[1].equals("1")) {
			throw new RuntimeException("convert sql config error, second val exp 0 or 1 act " + list[1]);
		}
		if(list.length < 4) {
			throw new RuntimeException("convert sql config error, exp limit 4 act " + String.valueOf(list.length));
		}
		SQLConfig config = new SQLConfig();
		String serverStr = list[2];
		String[] serverList = serverStr.split(",");
		if(serverList.length > 1) {
			config.setServer(serverList[0]);
			config.setPort(Integer.valueOf(serverList[1]));
		} else {
			config.setServer(serverList[0]);
			config.setPort(null);
		}
		config.setUsername(list[3]);
		if(list.length >= 5) {
			config.setPassword(list[4]);			
		} else {
			config.setPassword("");
		}
		return config;
	}
	
	private static byte[] getSvrSocketReturn(String server, int port, int appType, int timeout) throws UnknownHostException, IOException  {
		byte[] msg = getSocketMsg(appType);
		
		Socket socket = null;
		OutputStream os = null;
		DataOutputStream dos = null;
		InputStream is = null;
		byte[] result = null;
		
		try {
			socket = new Socket(server, port);
			socket.setSoTimeout(timeout);
					
			os = socket.getOutputStream();
			dos = new DataOutputStream(os);
	
			dos.write(msg);
			dos.flush();
			
			is = socket.getInputStream();
					
			result = readInputStream(is);
	
		} finally {
			if(is != null) {
				try {
					is.close();
				} catch (IOException e) {}
			}
			if(dos != null) {
				try {
					dos.close();
				} catch (IOException e) {}
			}
			if(os != null) {
				try {
					os.close();
				} catch (IOException e) {}
			}
			if(socket != null) {
				try {
					socket.close();
				} catch (IOException e) {}
			}
		}
	    return result;
	}
	
	private static  byte[] readInputStream(InputStream dis) throws IOException {
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		byte[] buffer = new byte[1024];
		int len = -1;
		byte[] result = null;
		boolean flag = false;
		try {
			do {
				len = dis.read(buffer);
				if(!flag && buffer[0] == chEdge) flag = true;
				if(len > 0 && flag) {
					bos.write(buffer, 0, len);
				}
			}while(len > 0 && len == buffer.length && buffer[len-1] != chEdge);
			result = bos.toByteArray();	
		} finally {
			try {
				bos.close();
			} catch (IOException e) {}			
		}
		return result;
    }
	
	private static byte[] getSocketMsg(int appType) {

		String computerIp = "127.0.0.1";
		String computerName = "tool";
		
		byte[] msg = new byte[0];
		msg = byteMerger(msg, chEdge);
		msg = byteMerger(msg, "CONNECT".getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, String.valueOf(appType).getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, String.valueOf(0).getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, computerIp.getBytes());
		msg = byteMerger(msg, chSplit);
		msg = byteMerger(msg, computerName.getBytes());
		msg = byteMerger(msg, chEdge);
		
		return msg;
	}
		
	private static byte[] byteMerger(byte[] bt1, byte[] bt2){  
        byte[] bt3 = new byte[bt1.length+bt2.length];  
        System.arraycopy(bt1, 0, bt3, 0, bt1.length);  
        System.arraycopy(bt2, 0, bt3, bt1.length, bt2.length);  
        return bt3;  
    } 
	
	private static byte[] byteMerger(byte[] bt, byte b) {
		byte[] btr = new byte[bt.length + 1];
		System.arraycopy(bt, 0, btr, 0, bt.length);
		btr[btr.length - 1] = b;
		return btr;
	}
}
