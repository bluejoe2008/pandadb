
import java.io.{File, FileInputStream}
import java.util.Properties

import cn.pandadb.server.{MainServerContext, PNodeServer}
import org.junit.{After, AfterClass, Assert, Before, BeforeClass, Test}
import org.neo4j.graphdb.factory.GraphDatabaseFactory
import org.neo4j.graphdb.{GraphDatabaseService, Result}
import org.neo4j.io.fs.FileUtils
import cn.pandadb.externalprops._
import org.neo4j.values.storable.{DateTimeValue, DateValue, LocalDateTimeValue, TimeValue}

trait QueryTestBase {
  var db: GraphDatabaseService = null
  val nodeStore = "InMemoryPropertyNodeStore"

  @Before
  def initdb(): Unit = {
    PNodeServer.toString
    new File("./output/testdb").mkdirs();
    FileUtils.deleteRecursively(new File("./output/testdb"));
    db = new GraphDatabaseFactory().newEmbeddedDatabaseBuilder(new File("./output/testdb")).
      newGraphDatabase()
    nodeStore match {
      case "InMemoryPropertyNodeStore" =>
        InMemoryPropertyNodeStore.nodes.clear()
        ExternalPropertiesContext.put(classOf[CustomPropertyNodeStore].getName, InMemoryPropertyNodeStore)
        MainServerContext.put("is.leader.node", true)

      case "InSolrPropertyNodeStore" =>
        val configFile = new File("./testdata/neo4j.conf")
        val props = new Properties()
        props.load(new FileInputStream(configFile))
        val zkString = props.getProperty("external.properties.store.solr.zk")
        val collectionName = props.getProperty("external.properties.store.solr.collection")
        val solrNodeStore = new InSolrPropertyNodeStore(zkString, collectionName)
        solrNodeStore.clearAll()
        ExternalPropertiesContext.put(classOf[CustomPropertyNodeStore].getName, solrNodeStore)
    }
  }

  @After
  def shutdowndb(): Unit = {
    db.shutdown()
  }

  protected def testQuery[T](query: String): Unit = {
    val tx = db.beginTx();
    val rs = db.execute(query);
    while (rs.hasNext) {
      val row = rs.next();
    }
    tx.success();
    tx.close()
  }
}
