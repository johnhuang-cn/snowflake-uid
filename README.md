# Snowflake-UID Generator

UidGenerator is a Java implemented, Snowflake based unique ID generator. It referenced the baidu's implementaton of [https://github.com/baidu/uid-generator](https://github.com/baidu/uid-generator).  But this generator is a simpler and non-db implementation, the default generator uses the last 24bit value of the ip address as the worker id, 28 bits for timestamp, and 11 bits for sequence. So it can generate 2048 sequence/second for each instance for about 8.7 years.

The kubernetes pod CIDR is less than /24, so this generator is suitable for k8s environment. If you can set the runtime network CIDR as /16, you can extend the time bits to 32 and sequence bits to 15, then the generator can generate 32768 sequences/seconds for each instance for about 128 years.

---

## 中文说明：

UidGenerator是基于Twitter Snowflake算法的分布式ID生成器，参考了百度的实现[https://github.com/baidu/uid-generator](https://github.com/baidu/uid-generator). 不过相对来说要更简单并且不依赖于DB。其默认实现使用28位的时间，24位的worker id和最后的11位用于生成序列。提供单实例每秒2048个序列号，可使用8.7年。

这么做的目的是为了让其适合在K8S环境中使用，因为K8S默认的CIDR是/24，所以使用24位worker id可保证每个实例生成的ID的唯一性。当然相应的，单例每秒的id数要少些，不过对大部分应用也是够的，而且通常高可用环境下每个应用不止一个实例。如果运行的k8s环境的CIDR可以设置为/16，那么可以将时间和序列的位数各加的，这样就可以支持每秒32768个序列，并能使用128年。

## Snowflake

\*\* Snowflake algorithm：\*\* An unique id consists of worker node, timestamp and sequence within that timestamp. Usually, it is a 64 bits number\(long\), and the default bits of that three fields are as follows:

| sign | delta seconds | worker node id | sequence |
| :---: | :---: | :---: | :---: |
| 1 bit | 28 bits | 24bits | 11bits |

* sign\(1bit\)  
  The highest bit is always 0.

* delta seconds \(28 bits\)  
  The next 28 bits, represents delta seconds since a customer epoch\(2018-07-01\). The maximum time will be 8.7 years.

* worker id \(24 bits\)  
  The next 24 bits, represents the worker node id, maximum value will be 16.7 million. Why set 24 bits instead of the 22 bits of baidu's implementation is because the default CIDR of k8s env is /24. 24 bits can guarantee that each instance has an unique worker id. If you can set the network CIDR to /16, you can decrease the worker id bits, and extend the time bits and sequence bits.

* sequence \(11 bits\)  
  the last 11 bits, represents sequence within the one second, maximum is 2048 per second for one instance by default.

## Quick Start

* ### Download the project and import

  This project haven't post to maven repo, so please download/clone this project and run "mvn install" to install it to local maven repo.

* ### Add maven dependency to project

  ```
  <dependency>
     <groupId>net.xdevelop</groupId>
     <artifactId>snowflake-uid</artifactId>
     <version>1.0.0</version>
  </dependency>
  ```
* ### Init the bean

  ```
  @Configuration
  public class UIDConfig {
      @Bean
      public SnowflakeUidGenerator customerUidGenerator() {
          long workerId = SnowflakeUidGenerator.getWorkerIdByIP(24);
          return new SnowflakeUidGenerator(workerId);
      }

      // init multiple uid generators for different DB tables
      @Bean
      public SnowflakeUidGenerator orderUidGenerator() {
          long workerId = SnowflakeUidGenerator.getWorkerIdByIP(24);
          return new SnowflakeUidGenerator(workerId);
      }
  }
  ```
* ### Generate the uid

  ```
  @Component
  public class CustomerService {
      @Autowired
      CustomerMapper mapper;

      @Autowired
      @Qualifier("customerUidGenerator")
      SnowflakeUidGenerator uidGenerator;

      public void addCustomer(Customer customer) {
          customer.setCustomerId(uidGenerator.getUID());
          long curTime = System.currentTimeMillis();
          customer.setCreatedDate(curTime);
          customer.setLastModifiedDate(curTime);
          mapper.insert(customer);
      }
  }
  ```
* ### Optional: customize the generator

  ```
  @Configuration
  public class UIDConfig {
      @Bean
      public SnowflakeUidGenerator customerUidGenerator() {
          long workerId = SnowflakeUidGenerator.getWorkerIdByIP(16);
          String baseDate = "2018-08-01";
          int timeBits = 32;
          int workerBits = 16;
          int seqBits = 15;
          return new SnowflakeUidGenerator(workerId, baseDate, timeBits, workerBits, seqBits);
      }
  }
  ```



