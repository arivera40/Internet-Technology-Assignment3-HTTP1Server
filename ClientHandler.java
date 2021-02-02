import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.net.Socket;

public class ClientHandler extends Thread {
	InputStream input_stream;
	OutputStream output_stream;
	Socket socket;
	HttpParser parser;

	public ClientHandler(Socket socket, InputStream dis, OutputStream dos, HttpParser parser) {
		this.socket = socket;
		this.input_stream = dis;
		this.output_stream = dos;
		this.parser = parser;
	}

	// Entry point when new thread is created and begins execution
	@Override
	public void run() {
		try {
			// HTTP server responses are written into the OutputStream - we use PrintWriter to output response header (and POST response body)
			// and we use BufferedOutputStream to output response body/data when necessary (for GET and HEAD requests)
			PrintWriter responseHead = new PrintWriter(new OutputStreamWriter (output_stream));

			String request = getClientRequest(responseHead);
			//if no incoming requests within 5 seconds, request will equal the empty string
			if(request.equals("")) {
				sleep(250);
				responseHead.close();
				input_stream.close();
				output_stream.close();
				socket.close();
				return;
			}
			
			BufferedOutputStream outStreamWriter = new BufferedOutputStream(output_stream);

			int status = parser.parseRequest(request);
			if (status == -1) {
				// respond with 404 Bad Request
				responseHead.print("HTTP/1.0 400 Bad Request" + "\r\n");
				responseHead.flush();
			} else if (status == -2) {
				// respond with 501 Not Implemented
				responseHead.print("HTTP/1.0 501 Not Implemented" + "\r\n");
				responseHead.flush();
			} else if (status == -3) {
				// respond with HTTP Version Not Supported
				System.out.println("Enters Version not supported\n");
				responseHead.print("HTTP/1.0 505 HTTP Version Not Supported" + "\r\n");
				responseHead.flush();
			} else if (status == -4){
				// respond with 500 Internal Server Error
				responseHead.println("HTTP/1.0 500 Internal Server Error" + "\r\n");
				responseHead.flush();
			} else if(status == -5){
				// respond with 411 Length Required
				responseHead.println("HTTP/1.0 411 Length Required" + "\r\n");
				responseHead.flush();
			}else if(status == -6){
				// respond with 405 Method Not Allowed
				responseHead.println("HTTP/1.0 405 Method Not Allowed" + "\r\n");
				responseHead.flush();
			}else {
				parser.getHttpResponse(responseHead, outStreamWriter);
			}
			//wait quarter second
			sleep(250);
			//PrintWriter and BufferedOutputStream already flush()'ed after use
			//Close down communication object
			responseHead.close();
			outStreamWriter.close();
			input_stream.close();
			output_stream.close();
			socket.close();

		} catch (Exception e) {
			e.printStackTrace();
		}

	}
	
	//Reads in clientRequest from the input stream
	private String getClientRequest(PrintWriter response) throws IOException {
		StringBuilder request = new StringBuilder();
		boolean first = true;
		do {
			if(first) {
				try {
					//Waits 5 seconds and if no incoming request within that time responds with 408 Request Timeout
					sleep(5000);
					if(input_stream.available() <= 0) {
						response.print("HTTP/1.0 408 Request Timeout" + "\r\n");
						response.flush();
						return "";
						
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
			request.append((char) input_stream.read());
			first = false;
		}while(input_stream.available() > 0);
		return request.toString();
	}
}
