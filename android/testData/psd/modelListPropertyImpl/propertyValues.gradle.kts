val propB by extra("2")
val propC by extra("3")
val propRef by extra(propB)
val propInterpolated by extra("${propB}nd")
val propList by extra(listOf("1", propB, propC, propRef, propInterpolated))
val propList2 by extra{listOf("1")}
val propListRef by extra(propList)
