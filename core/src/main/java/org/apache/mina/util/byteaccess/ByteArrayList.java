/*
 *  Licensed to the Apache Software Foundation (ASF) under one
 *  or more contributor license agreements.  See the NOTICE file
 *  distributed with this work for additional information
 *  regarding copyright ownership.  The ASF licenses this file
 *  to you under the Apache License, Version 2.0 (the
 *  "License"); you may not use this file except in compliance
 *  with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing,
 *  software distributed under the License is distributed on an
 *  "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 *  KIND, either express or implied.  See the License for the
 *  specific language governing permissions and limitations
 *  under the License.
 *
 */
package org.apache.mina.util.byteaccess;

import java.util.NoSuchElementException;

/**
 * A linked list that stores <code>ByteArray</code>s and maintains several useful invariants.
 *
 * TODO: Explain invariants.
 */
class ByteArrayList {

    /**
     * A {@link Node} which indicates the start and end of the list and does not
     * hold a value. The value of <code>next</code> is the first item in the
     * list. The value of of <code>previous</code> is the last item in the list.
     */
    private final Node header;

    private int firstByte;

    private int lastByte;

    protected ByteArrayList() {
        header = new Node();
    }

    public int lastByte() {
        return lastByte;
    }

    public int firstByte() {
        return firstByte;
    }

    public boolean isEmpty() {
        return header.next == header;
    }

    public Node getFirst() {
        return header.getNextNode();
    }

    public Node getLast() {
        return header.getPreviousNode();
    }

    public void addFirst(ByteArray ba) {
        addNode(new Node(ba), header.next);
        firstByte -= ba.last();
    }

    public void addLast(ByteArray ba) {
        addNode(new Node(ba), header);
        lastByte += ba.last();
    }

    public Node removeFirst() {
        Node node = header.getNextNode();
        firstByte += node.ba.last();
        return removeNode(node);
    }

    public Node removeLast() {
        Node node = header.getPreviousNode();
        lastByte -= node.ba.last();
        return removeNode(node);
    }

    //-----------------------------------------------------------------------

    /**
     * Inserts a new node into the list.
     *
     * @param nodeToInsert  new node to insert
     * @param insertBeforeNode  node to insert before
     * @throws NullPointerException if either node is null
     */
    protected void addNode(Node nodeToInsert, Node insertBeforeNode) {
        // Insert node.
        nodeToInsert.next = insertBeforeNode;
        nodeToInsert.previous = insertBeforeNode.previous;
        insertBeforeNode.previous.next = nodeToInsert;
        insertBeforeNode.previous = nodeToInsert;
    }

    /**
     * Removes the specified node from the list.
     *
     * @param node  the node to remove
     * @throws NullPointerException if <code>node</code> is null
     */
    protected Node removeNode(Node node) {
        // Remove node.
        node.previous.next = node.next;
        node.next.previous = node.previous;
        node.removed = true;
        return node;
    }

    //-----------------------------------------------------------------------
    /**
     * A node within the linked list.
     * <p>
     * From Commons Collections 3.1, all access to the <code>value</code> property
     * is via the methods on this class.
     */
    public class Node {

        /** A pointer to the node before this node */
        private Node previous;
        /** A pointer to the node after this node */
        private Node next;
        /** The ByteArray contained within this node */
        private ByteArray ba;
        private boolean removed;

        /**
         * Constructs a new header node.
         */
        private Node() {
            super();
            previous = this;
            next = this;
        }

        /**
         * Constructs a new node with a value.
         */
        private Node(ByteArray ba) {
            super();
            if (ba == null) {
                throw new NullPointerException("ByteArray must not be null.");
            }
            this.ba = ba;
        }

        /**
         * Gets the previous node.
         *
         * @return the previous node
         */
        public Node getPreviousNode() {
            if (!hasPreviousNode()) {
                throw new NoSuchElementException();
            }
            return previous;
        }

        /**
         * Gets the next node.
         *
         * @return the next node
         */
        public Node getNextNode() {
            if (!hasNextNode()) {
                throw new NoSuchElementException();
            }
            return next;
        }

        public boolean hasPreviousNode() {
            return previous != header;
        }

        public boolean hasNextNode() {
            return next != header;
        }

        public ByteArray getByteArray() {
            return ba;
        }

        public boolean isRemoved() {
            return removed;
        }
    }

}
