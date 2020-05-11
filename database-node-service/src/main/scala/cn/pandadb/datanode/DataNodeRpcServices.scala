package cn.pandadb.datanode

import org.neo4j.graphdb.Direction

case class SayHello(msg: String)

// node
case class CreateNode(labels: Array[String], properties: Map[String, Any])

case class AddNodeLabel(id: Long, label: String)

case class GetNodeById(id: Long)

case class GetNodesByProperty(label: String, propertiesMap: Map[String, Object])

case class GetNodesByLabel(label: String)

case class UpdateNodeProperty(id: Long, propertiesMap: Map[String, Any])

case class UpdateNodeLabel(id: Long, toDeleteLabel: String, newLabel: String)

case class DeleteNode(id: Long)

case class RemoveProperty(id: Long, property: String)

// relationship
case class CreateNodeRelationship(id1: Long, id2: Long, relationship: String, direction: Direction)

case class GetNodeRelationships(id: Long)

case class DeleteNodeRelationship(id: Long, relationship: String, direction: Direction)

case class GetAllDBNodes(chunkSize: Int)

case class GetAllDBRelationships(chunkSize: Int)