package network.message

class NodeMessage(var senderId: Int, var receiverId: Int, val type: MessageType, val payload: String) {}