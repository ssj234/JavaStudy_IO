package com.ssj.netty.custom.protocol;

public class CMessage {

	private CHeader header;
	private String data;
	
	public  CMessage(){
		
	}
	
	public  CMessage(CHeader header){
		this.header=header;
	}
	
	public CMessage(CHeader header, String data) {
		this.header = header;
		this.data = data;
	}

	public CHeader getHeader() {
		return header;
	}

	public void setHeader(CHeader header) {
		this.header = header;
	}

	public String getData() {
		return data;
	}

	public void setData(String data) {
		this.data = data;
	}
	
	
}
