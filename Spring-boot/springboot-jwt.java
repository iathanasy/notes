---------------------
jwt
---------------------
	# Json Web Token
		https://jwt.io/
		https://github.com/jwtk/jjwt
	
	# JWT包含了三部分
		Header 头部(标题包含了令牌的元数据, 并且包含签名和/或加密算法的类型)
			{ 
				"alg": "HS256",
				"typ": "JWT"
			} 

			* 头部存在两个信息, token类型和采用的加密算法
			* 加密的算法通常使用: HMAC, SHA256, MD5
			* 还有几个不常用的头
				cty
				kid
			
			* 都定义在了接口:PublicClaims 中

		Payload 负载(请求体, 消息主体)

			{
			  "sub": "1234567890",			// 标准属性
			  "name": "John Doe",			// 私有的属性
			  "admin": true					// 私有属性
			}
			
			* Payload 部分也是一个 JSON 对象, 用来存放实际需要传递的数据
			* JWT 规定了7个官方字段, 都定义在了接口:PublicClaims 中
					iss (issuer)签发人
					exp (expiration time)过期时间
					sub (subject)主题
					aud (audience)受众
					nbf (Not Before)生效时间
					iat (Issued At)签发时间
					jti (JWT ID)编号

			* 还可以在这个部分定义私有字段
			* JWT 默认是不加密的, 任何人都可以读到, 所以不要把秘密信息放在这个部分

			
		Signature 签名/签证
			* 这个部分需要base64加密后的header和base64加密后的payload使用,
			* 连接组成的字符串, 然后通过header中声明的加密方式进行加盐secret组合加密, 然后就构成了jwt的第三部分
			* 密钥secret是保存在服务端的, 服务端会根据这个密钥进行生成token和进行验证, 所以需要保护好
	
	# Header
		Authorization: Bearer <token>

---------------------
jwt - springboot
---------------------
	# Maven
		<!-- https://mvnrepository.com/artifact/com.auth0/java-jwt -->
		<dependency>
			<groupId>com.auth0</groupId>
			<artifactId>java-jwt</artifactId>
			<version>3.10.1</version>
		</dependency>
	
	# 签发Token
		// 距今一个礼拜后的时间戳
		long expiresTimestamp = LocalDateTime.now().plusWeeks(1).toEpochSecond(ZoneOffset.UTC);
		
		// 使用私钥创建加密算法
		Algorithm  algorithm = Algorithm.HMAC256("HellWorld");
		
		String token = JWT.create()
			.withHeader(Map.ofEntries(Map.entry("alg", "HS256"), Map.entry("typ", "JWT")))
			
			.withClaim("name", "KevinBlandy")				// 添加一个或者多个自定义信息
			.withClaim("id", 1000L)
			
			.withIssuer("springboot中文社区")				// 签发人
			.withExpiresAt(new Date(expiresTimestamp))		// 过期时间
			.withSubject("") 								// 主题
			.withAudience("1", "2", "n") 					// 受众
			.withNotBefore(new Date()) 						// 生效时间
			.withIssuedAt(new Date()) 						// 签发时间
			.withJWTId("123456") 							// 编号
			.sign(algorithm);
	
	# Token 校验
		
		// DecodedJWT 包含了 header/body 等一系列的信息
		DecodedJWT decodedJWT = JWT.require(algorithm)
						.build().verify(token);
		
		// 标准信息
		String issuser = decodedJWT.getIssuer();
		Date expiresAt = decodedJWT.getExpiresAt();
		String subject = decodedJWT.getSubject();
		List<String> audiences = decodedJWT.getAudience();
		Date notBefore = decodedJWT.getNotBefore();
		Date issuedAt = decodedJWT.getIssuedAt();
		String id = decodedJWT.getId();
		
		// 私有信息
		String name = decodedJWT.getClaim("name").asString();
		long id = decodedJWT.getClaim("id").asLong();
		
		// 所有的私有信息
		Map<String, Claim> claims = decodedJWT.getClaims();
	

		// 不校验签名的情况下, 解析token
		DecodedJWT decodedJWT = JWT.decode(String token)

	#  如果token已经过期, 或者是校验失败, 会抛出异常
			JWTVerificationException
				|-AlgorithmMismatchException
				|-InvalidClaimException
				|-JWTCreationException
				|-JWTDecodeException
				|-SignatureGenerationException
				|-SignatureVerificationException
				|-TokenExpiredException


---------------------
jwt - RSA
---------------------
	# RSA
		* 私钥只能用来签名JWT，不能用来校验它。
		* 第二个密钥叫做公钥(public key)，是应用服务器使用来校验JWT。
		* 公钥可以用来校验JWT，但不能用来给JWT签名。
		* 公钥一般不需要严密保管，因为即便黑客拿到了，也无法使用它来伪造签名。

		* 场景就是，可以把公钥分发给别人，别人可以用来校验Token是否由系统的私钥签发


	# Demo
		import java.security.KeyPair;
		import java.security.KeyPairGenerator;
		import java.security.NoSuchAlgorithmException;
		import java.security.interfaces.RSAPrivateKey;
		import java.security.interfaces.RSAPublicKey;
		import java.util.Date;
		import java.util.HashMap;
		import java.util.Map;

		import com.auth0.jwt.JWT;
		import com.auth0.jwt.algorithms.Algorithm;
		import com.auth0.jwt.interfaces.DecodedJWT;

		public class JWTMain {
			public static void main(String[] args) throws Exception {
				
				// 创建私钥和公钥
				KeyPair keyPair = initKey();
				RSAPublicKey rsaPublicKey = (RSAPublicKey) keyPair.getPublic();
				RSAPrivateKey rsaPrivateKey = (RSAPrivateKey) keyPair.getPrivate();

				// 使用公钥/私钥创建 Algorithm
				Algorithm algorithm = Algorithm.RSA512(rsaPublicKey, rsaPrivateKey);
				
				Map<String, Object> jwtHeader = new HashMap<>();
				jwtHeader.put("alg", "RS512");  // 指定RSA512
				jwtHeader.put("typ", "JWT");

				// 生成Token
				String token = JWT.create().withHeader(jwtHeader)
						.withClaim("id", 1000L)
						.withIssuer("springboot中文社区") // 签发人
						.withNotBefore(new Date()) // 生效时间
						.withIssuedAt(new Date()) // 签发时间
						.withJWTId("123456") // 编号
						.sign(algorithm);
				
				System.out.println(token);
				
				// 校验Token
				DecodedJWT decodedJWT = JWT.require(algorithm)
						.build().verify(token);
				
				System.out.println(decodedJWT);
				
			}

			// 初始化公钥和密钥
			public static KeyPair initKey() throws NoSuchAlgorithmException {
				KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA"); // RSA 加密
				keyPairGenerator.initialize(2048); // RSA秘钥长度
				KeyPair keyPair = keyPairGenerator.generateKeyPair();
				return keyPair;
			}
		}
