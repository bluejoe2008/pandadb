package cn.pandadb.server

import cn.pandadb.network.{ClusterClient, NodeAddress}
import cn.pandadb.util._

class MainServerModule extends PandaModule {
  override def init(ctx: PandaModuleContext): Unit = {
    val conf = ctx.configuration;
    import ConfigUtils._
    MainServerContext.put("node.server.address", NodeAddress.fromString(conf.getRequiredValueAsString("node.server.address")));
  }

  override def stop(ctx: PandaModuleContext): Unit = {

  }

  override def start(ctx: PandaModuleContext): Unit = {

  }
}

object MainServerContext extends ContextMap {
  def bindMasterRole(role: MasterRole): Unit = {
    super.put("isLeaderNode", true)
    super.put[MasterRole](role);
  }

  def bindDataLogRedaerWriter(logReader: DataLogReader, logWriter: DataLogWriter): Unit = {
    super.put[DataLogReader](logReader)
    super.put[DataLogWriter](logWriter)
  }

  def dataLogWriter: DataLogWriter = super.get[DataLogWriter]
  def dataLogReader: DataLogReader = super.get[DataLogReader]

  def nodeAddress: NodeAddress = get("node.server.address");

  def masterRole: MasterRole = super.get[MasterRole]

  def clusterClient: ClusterClient = super.get[ClusterClient]

  def isLeaderNode: Boolean = super.get("isLeaderNode")
}