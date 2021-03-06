package cn.pandadb.server

import cn.pandadb.network.{ClusterClient, NodeAddress, ZKPathConfig}
import cn.pandadb.util._

class MainServerModule extends PandaModule {
  override def init(ctx: PandaModuleContext): Unit = {
    val conf = ctx.configuration;
    import ConfigUtils._
    MainServerContext.bindNodeAddress(NodeAddress.fromString(conf.getRequiredValueAsString("node.server.address")));
    MainServerContext.bindZKServerAddressStr(conf.getRequiredValueAsString("zookeeper.address"))
    MainServerContext.bingRpcPort(conf.getRequiredValueAsInt("rpc.port"))
    ZKPathConfig.initZKPath(MainServerContext.zkServerAddressStr)
  }

  override def close(ctx: PandaModuleContext): Unit = {

  }

  override def start(ctx: PandaModuleContext): Unit = {

  }
}

object MainServerContext extends ContextMap {
  def bindMasterRole(role: MasterRole): Unit = {
    GlobalContext.setLeaderNode(true)
    super.put[MasterRole](role);
  }

  def bindDataLogReaderWriter(logReader: DataLogReader, logWriter: DataLogWriter): Unit = {
    super.put[DataLogReader](logReader)
    super.put[DataLogWriter](logWriter)
  }

  def bindZKServerAddressStr(zkAddressString: String): Unit = put("zookeeper.address", zkAddressString)

  def dataLogWriter: DataLogWriter = super.get[DataLogWriter]

  def dataLogReader: DataLogReader = super.get[DataLogReader]

  def bindNodeAddress(nodeAddress: NodeAddress): Unit = put("node.server.address", nodeAddress);

  def bingRpcPort(port: Int): Unit = put("rpcPort", port)

  def rpcPort: Int = get("rpcPort")

  def nodeAddress: NodeAddress = get("node.server.address");

  def zkServerAddressStr: String = get("zookeeper.address");

  def masterRole: MasterRole = super.get[MasterRole]

  def clusterClient: ClusterClient = super.get[ClusterClient]
}