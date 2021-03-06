package cn.pandadb.driver

import java.io.IOException
import java.net.URI
import java.security.GeneralSecurityException
import java.util.Collections
import java.{security, util}
import java.util.concurrent.{CompletableFuture, CompletionStage}

import cn.pandadb.cypherplus.utils.CypherPlusUtils
import cn.pandadb.network.{ClusterClient, NodeAddress, ZookeeperBasedClusterClient}
import org.apache.commons.lang3.NotImplementedException
import org.neo4j.driver.Config.TrustStrategy
import org.neo4j.driver.{Transaction, Value, _}
import org.neo4j.driver.async.{AsyncSession, AsyncStatementRunner, AsyncTransaction, AsyncTransactionWork, StatementResultCursor}
import org.neo4j.driver.exceptions.ClientException
import org.neo4j.driver.internal.async.connection.BootstrapFactory
import org.neo4j.driver.internal.cluster.{RoutingContext, RoutingSettings}
import org.neo4j.driver.internal.{AbstractStatementRunner, BoltServerAddress, DirectConnectionProvider, DriverFactory, SessionConfig, SessionFactory, SessionFactoryImpl}
import org.neo4j.driver.internal.metrics.{InternalMetricsProvider, MetricsProvider}
import org.neo4j.driver.internal.retry.{ExponentialBackoffRetryLogic, RetryLogic, RetrySettings}
import org.neo4j.driver.internal.security.SecurityPlan
import org.neo4j.driver.internal.spi.ConnectionProvider
import org.neo4j.driver.internal.types.InternalTypeSystem
import org.neo4j.driver.internal.util.{Clock, Extract, Futures}
import org.neo4j.driver.internal.value.MapValue
import org.neo4j.driver.reactive.{RxSession, RxStatementResult, RxTransaction, RxTransactionWork}
import org.neo4j.driver.types.TypeSystem
import org.reactivestreams.Publisher

import scala.collection.JavaConversions
import cn.pandadb.driver.PandaTransaction

import scala.util.matching.Regex

/**
 * Created by bluejoe on 2019/11/21.
 */
object PandaDriver {
  def create(uri: String, authToken: AuthToken, config: Config): Driver = {
    new PandaDriver(uri, authToken, config)
  }
}

class PandaDriver(uri: String, authToken: AuthToken, config: Config) extends Driver {
  val clusterClient: ClusterClient = createClusterClient(uri);
  //  val defaultSessionConfig = new SessionConfig()
  val defaultSessionConfig = SessionConfig.empty()
  override def closeAsync(): CompletionStage[Void] = {
    //TODO
    new CompletableFuture[Void]();
  }

  override def session(): Session = session(defaultSessionConfig)

  override def session(sessionConfig: SessionConfig): Session = new PandaSession(sessionConfig, clusterClient);

  override def defaultTypeSystem(): TypeSystem = InternalTypeSystem.TYPE_SYSTEM

  override def rxSession(): RxSession = {
    this.rxSession(defaultSessionConfig)
  }

  override def rxSession(sessionConfig: SessionConfig): RxSession = {
    throw new NotImplementedException("rxSession")
  }

  /**
   *   verifyConnectivityAsync and verifyConnectivity  is not right ,  because uri is zkString
   */

  override def verifyConnectivityAsync(): CompletionStage[Void] = {
    throw new NotImplementedException("verifyConnectivityAsync")
  }

  override def verifyConnectivity(): Unit = {
    Futures.blockingGet(this.verifyConnectivityAsync())
  }

  override def metrics(): Metrics = {
    createDriverMetrics(config, this.createClock()).metrics()
  }
  private def createDriverMetrics(config: Config, clock: Clock ): MetricsProvider = {
    if (config.isMetricsEnabled()) new InternalMetricsProvider(clock) else MetricsProvider.METRICS_DISABLED_PROVIDER
  }
  private def createClock(): Clock = {
    Clock.SYSTEM
  }

  override def asyncSession(): AsyncSession = {
    this.asyncSession(defaultSessionConfig)
  }

  override def asyncSession(sessionConfig: SessionConfig): AsyncSession = {
    throw new NotImplementedException("asyncSession")
  }

  override def close(): Unit = {

  }

  //wait to finish
  override def isEncrypted: Boolean = {
    throw new NotImplementedException("isEncrypted")
  }

  private def createClusterClient(uri: String): ClusterClient = {
    //scalastyle:off
    val pattern = new Regex("((2[0-4]\\d|25[0-5]|[01]?\\d\\d?)\\.){3}(2[0-4]\\d|25[0-5]|[01]?\\d\\d?):[0-9]{1,5}")
    val zkString = (pattern findAllIn uri).mkString(",")
    new ZookeeperBasedClusterClient(zkString)
  }
}

class PandaSession(sessionConfig: SessionConfig, clusterOperator: ClusterClient) extends Session {

  var session: Session = null
  var readDriver: Driver = null
  var writeDriver : Driver = null
  private def getSession(isWriteStatement: Boolean): Session = {
    if (!(this.session==null)) this.session.close()
    if (isWriteStatement) {
      if (this.writeDriver==null) this.writeDriver = SelectNode.getDriver(isWriteStatement, clusterOperator)
      this.session = this.writeDriver.session(sessionConfig)
    } else {
      if (this.readDriver==null) this.readDriver = SelectNode.getDriver(isWriteStatement, clusterOperator)
      this.session = this.readDriver.session(sessionConfig)
    }
    this.session
  }

  override def writeTransaction[T](work: TransactionWork[T]): T = {
    this.writeTransaction(work, TransactionConfig.empty())
  }


  override def writeTransaction[T](work: TransactionWork[T], config: TransactionConfig): T = {
    getSession(true).writeTransaction(work, config)
  }


  override def readTransaction[T](work: TransactionWork[T]): T = {
    this.readTransaction(work, TransactionConfig.empty())
  }

  override def readTransaction[T](work: TransactionWork[T], config: TransactionConfig): T = {
    getSession(false).readTransaction(work, config)
  }

  override def run(statement: String, config: TransactionConfig): StatementResult = {
    this.run(statement, Collections.emptyMap(), config)
  }

  override def run(statement: String, parameters: util.Map[String, AnyRef], config: TransactionConfig): StatementResult = {
    this.run(new Statement(statement, parameters), config)
  }

  override def run(statement: Statement, config: TransactionConfig): StatementResult = {
    val tempState = statement.text().toLowerCase()
    val isWriteStatement = CypherPlusUtils.isWriteStatement(tempState)
    getSession(isWriteStatement)
    this.session.run(statement, config)
  }

  override def close(): Unit = {
    if (!(this.session == null)) session.close()
    if (!(this.writeDriver == null)) this.writeDriver.close()
    if (!(this.readDriver == null)) this.readDriver.close()
  }

  override def lastBookmark(): String = {
    session.lastBookmark()
  }

  override def reset(): Unit = {
    session.reset()
  }

  override def beginTransaction(): Transaction = {
    this.beginTransaction(TransactionConfig.empty())
  }

  override def beginTransaction(config: TransactionConfig): Transaction = {
    /*isTransaction = true
    this.config = config
    this.transaction*/
    new PandaTransaction(sessionConfig, config, clusterOperator)
  }

  override def run(statementTemplate: String, parameters: Value): StatementResult = {
    this.run(new Statement(statementTemplate, parameters))
  }

  override def run(statementTemplate: String, statementParameters: util.Map[String, AnyRef]): StatementResult = {
    this.run(statementTemplate, AbstractStatementRunner.parameters(statementParameters))
  }

  override def run(statementTemplate: String, statementParameters: Record): StatementResult = {
    //session.run(statementTemplate, statementParameters) AbstractStatementRunner
    //this.run(statementTemplate, parameters(statementParameters))
    this.run(statementTemplate, AbstractStatementRunner.parameters(statementParameters))
  }

  override def run(statementTemplate: String): StatementResult = {
    this.run(statementTemplate, Values.EmptyMap)
  }

  override def run(statement: Statement): StatementResult = {
    //session.run(statement)
    this.run(statement, TransactionConfig.empty())
  }

  override def isOpen: Boolean = {
    session.isOpen
  }
}