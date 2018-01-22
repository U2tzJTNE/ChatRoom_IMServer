package service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.Iterator;

import com.google.gson.Gson;

import db.DBManager;
import entity.MessageEntity;

public class ServerThread implements Runnable {
	
	Socket s;
	BufferedReader br;
	private String APOSTROPHE = "%u0027%";

	public ServerThread(Socket s) throws IOException {
		this.s = s;
		this.br = new BufferedReader(new InputStreamReader(s.getInputStream(), "utf-8"));
	}

	public void run() {
		try {
			String content = null;
			Iterator<Socket> it;
			for (; (content = readFromClient()) != null; it.hasNext()) {
				MessageEntity mEntity = new Gson().fromJson(content, MessageEntity.class);
				if (mEntity!=null) {
					String nickname = mEntity.getNickname();
					String avatarURL = mEntity.getAvatarURL();
					String date = mEntity.getDate();
					String message = mEntity.getMessage().replaceAll("'", APOSTROPHE);
					String userId = mEntity.getUserId();

					// 获取Sql查询语句
					String addSql = "insert into message(nickname,avatar_url,user_id,date,message) values('"+ nickname + "','" + avatarURL + "','" + userId + "','" + date + "','" + message + "') ";
					System.out.println("-------------------"+addSql);
					// 获取DB对象
					DBManager sql = DBManager.createInstance();
					sql.connectDB();

					int ret = sql.executeUpdate(addSql);
					if (ret != 0) {
						//插入成功
						System.out.println("------------>>插入成功<<-------------");
					}else {
						//插入失败
						System.out.println("------------>>插入失败<<-------------");
					}
					sql.closeDB();
				}
				it = MyServer.socketList.iterator();
				s = it.next();
				try {
					OutputStream os = s.getOutputStream();
					os.write((content + "\n").getBytes("utf-8"));
					System.out.println("------------>>向客户端发送数据<<-------------");
				} catch (SocketException e) {
					it.remove();
					System.out.println("------------>>用户退出登录<<-------------");
				}
			}

		} catch (IOException e) {
			System.out.println("------------>>向客户端发送数据失败<<-------------");
			MyServer.socketList.remove(this.s);
		}
	}
	
	private String readFromClient() {
		try {
			return this.br.readLine();
		} catch (IOException e) {
			System.out.println("------------>>读取客户端数据失败<<-------------");
			MyServer.socketList.remove(this.s);
		}
		return null;
	}
}