package cn.pandadb.jraft.rpc.values

object Direction extends Enumeration {
  val OUTGOING = Value(0)
  val INCOMING = Value(1)
  val BOTH = Value(2)
}
