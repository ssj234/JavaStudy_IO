package net.ssj.aio.client;

import java.text.SimpleDateFormat;
import java.sql.Date;

public class Test {

	
	public static void main(String[] args) {
		Date date=new Date(System.currentTimeMillis());
//		System.out.println(date.UTC());
		print(date);
		
		java.util.Date start=(java.util.Date) date.clone();
		start.setHours(0);
		start.setMinutes(0);
		start.setSeconds(0);
		print(start);
		
		java.util.Date end=(java.util.Date) date.clone();
		end.setHours(23);
		end.setMinutes(59);
		end.setSeconds(59);
		print(end);
	}
	
	public static void print(Date date){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.format(date));
	}
	
	public static void print(java.util.Date date){
		SimpleDateFormat sdf=new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		System.out.println(sdf.format(date));
	}
}
