package com.hoster.image;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Date;

import javax.annotation.PostConstruct;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.CannedAccessControlList;
import com.amazonaws.services.s3.model.DeleteObjectRequest;
import com.amazonaws.services.s3.model.PutObjectRequest;

@Service
public class AmazonClient {
	
	private AmazonS3 s3client;
	
	//value will bind application properties directly to class fields during initialization 
	@Value("${amazonProperties.endpointUrl}")
	private String endpointUrl;
	
	@Value("${amazonProperties.bucketName}")
	private String bucketName;
	
	@Value("${amazonProperties.accessKey}")
	private String accessKey;
	
	@Value("${amazonProperties.secretKey}")
	private String secretKey;
	
	@PostConstruct //fields are null in constructor
	private void initializeAmazon() {//set amazon credentials to amazon client
		BasicAWSCredentials credentials = new BasicAWSCredentials(this.accessKey, this.secretKey);
		this.s3client = AmazonS3ClientBuilder.standard().withRegion(Regions.US_EAST_1)
		        .withCredentials(new AWSStaticCredentialsProvider(credentials))
		        .build();
	}
	
	private File convertMultiPartToFile(MultipartFile file) throws IOException {//converts multipart file to file
		File convFile = new File(file.getOriginalFilename());
		FileOutputStream fos = new FileOutputStream(convFile);
		fos.write(file.getBytes());
		fos.close();
		return convFile;
	}
	
	private String generateFileName(MultipartFile multiPart) {//generates unique name for each file upload
		return new Date().getTime() + "-" + multiPart.getOriginalFilename().replace(" ", "_");
	}
	
	private void uploadFileTos3bucket(String fileName, File file) {//adding public read permissions
		s3client.putObject(
		    new PutObjectRequest(bucketName, fileName, file).withCannedAcl(CannedAccessControlList.PublicRead));
	}
	
	public String uploadFile(MultipartFile multipartFile) {//save to bucket and return url to store in database
		
		String fileUrl = "";
		try {
			File file = convertMultiPartToFile(multipartFile);
			String fileName = generateFileName(multipartFile);
			fileUrl = endpointUrl + "/" + bucketName + "/" + fileName;
			uploadFileTos3bucket(fileName, file);
			file.delete();
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		return fileUrl;
	}
	
	public String deleteFileFromS3Bucket(String fileUrl) {
		String fileName = fileUrl.substring(fileUrl.lastIndexOf("/") + 1);
		s3client.deleteObject(new DeleteObjectRequest(bucketName + "/", fileName));
		return "Successfully deleted";
	}
}
