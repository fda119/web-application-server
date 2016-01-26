package util;

import java.io.BufferedReader;
import java.io.IOException;

public class IOUtils {
	/**
	 * @param BufferedReader는 Request Body를 시작하는 시점이어야 
	 * @param contentLength는 Request Header의 Content-Length 값이다.
	 * @return
	 * @throws IOException
	 */
	//개행 문자 단위로 끊어 읽지 못하는 경우를 위해 사용함 
	public static String readData(BufferedReader br, int contentLength) throws IOException {
		char[] body = new char[contentLength];
		br.read(body, 0, contentLength);
		return String.copyValueOf(body);
	}
}
