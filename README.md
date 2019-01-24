# SimplrRpc
RPC框架
注册中心 Zookeper
序列化protolb uf
通信框架netty

姑且叫做zubbo


 /**
         * 监听数据节点的变化情况
         */
        final NodeCache nodeCache = new NodeCache(client, "/"+serviceName+"/provider", false);
        nodeCache.start(true);
        nodeCache.getListenable().addListener(
                new NodeCacheListener() {
                    @Override
                    public void nodeChanged() throws Exception {
                        System.out.println("Node data is changed, new data: " +
                                new String(nodeCache.getCurrentData().getData()));
                    }
                },
                pool
        );



         /**
                 * 监听子节点的变化情况
                 */
                final PathChildrenCache childrenCache = new PathChildrenCache(client, "/zk-huey", true);
                childrenCache.start(PathChildrenCache.StartMode.POST_INITIALIZED_EVENT);
                childrenCache.getListenable().addListener(
                        new PathChildrenCacheListener() {
                            @Override
                            public void childEvent(CuratorFramework client, PathChildrenCacheEvent event)
                                    throws Exception {
                                switch (event.getType()) {
                                    case CHILD_ADDED:
                                        System.out.println("CHILD_ADDED: " + event.getData().getPath());
                                        break;
                                    case CHILD_REMOVED:
                                        System.out.println("CHILD_REMOVED: " + event.getData().getPath());
                                        break;
                                    case CHILD_UPDATED:
                                        System.out.println("CHILD_UPDATED: " + event.getData().getPath());
                                        break;
                                    default:
                                        break;
                                }
                            }
                        },
                        pool
                );