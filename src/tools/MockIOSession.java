/*
This file is part of the OdinMS Maple Story Server
Copyright (C) 2008 Patrick Huy <patrick.huy@frz.cc> 
Matthias Butz <matze@odinms.de>
Jan Christian Meyer <vimes@odinms.de>

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU Affero General Public License version 3
as published by the Free Software Foundation. You may not use, modify
or distribute this program under any other version of the
GNU Affero General Public License.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package tools;

import org.apache.mina.core.filterchain.IoFilterChain;
import org.apache.mina.core.future.CloseFuture;
import org.apache.mina.core.future.ReadFuture;
import org.apache.mina.core.future.WriteFuture;
import org.apache.mina.core.service.IoHandler;
import org.apache.mina.core.service.IoService;
import org.apache.mina.core.service.TransportMetadata;
import org.apache.mina.core.session.IdleStatus;
import org.apache.mina.core.session.IoSession;
import org.apache.mina.core.session.IoSessionConfig;
import org.apache.mina.core.write.WriteRequest;
import org.apache.mina.core.write.WriteRequestQueue;

import java.net.SocketAddress;
import java.util.Set;

public class MockIOSession implements IoSession {
    @Override
    public long getId() {
        return 0;
    }

    @Override
    public IoService getService() {
        return null;
    }

    @Override
    public IoHandler getHandler() {
        return null;
    }

    @Override
    public IoSessionConfig getConfig() {
        return null;
    }

    @Override
    public IoFilterChain getFilterChain() {
        return null;
    }

    @Override
    public WriteRequestQueue getWriteRequestQueue() {
        return null;
    }

    @Override
    public TransportMetadata getTransportMetadata() {
        return null;
    }

    @Override
    public ReadFuture read() {
        return null;
    }

    @Override
    public WriteFuture write(Object o) {
        return null;
    }

    @Override
    public WriteFuture write(Object o, SocketAddress socketAddress) {
        return null;
    }

    @Override
    public CloseFuture close(boolean b) {
        return null;
    }

    @Override
    public CloseFuture close() {
        return null;
    }

    @Override
    public Object getAttachment() {
        return null;
    }

    @Override
    public Object setAttachment(Object o) {
        return null;
    }

    @Override
    public Object getAttribute(Object o) {
        return null;
    }

    @Override
    public Object getAttribute(Object o, Object o1) {
        return null;
    }

    @Override
    public Object setAttribute(Object o, Object o1) {
        return null;
    }

    @Override
    public Object setAttribute(Object o) {
        return null;
    }

    @Override
    public Object setAttributeIfAbsent(Object o, Object o1) {
        return null;
    }

    @Override
    public Object setAttributeIfAbsent(Object o) {
        return null;
    }

    @Override
    public Object removeAttribute(Object o) {
        return null;
    }

    @Override
    public boolean removeAttribute(Object o, Object o1) {
        return false;
    }

    @Override
    public boolean replaceAttribute(Object o, Object o1, Object o2) {
        return false;
    }

    @Override
    public boolean containsAttribute(Object o) {
        return false;
    }

    @Override
    public Set<Object> getAttributeKeys() {
        return null;
    }

    @Override
    public boolean isConnected() {
        return false;
    }

    @Override
    public boolean isClosing() {
        return false;
    }

    @Override
    public CloseFuture getCloseFuture() {
        return null;
    }

    @Override
    public SocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public SocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public SocketAddress getServiceAddress() {
        return null;
    }

    @Override
    public void setCurrentWriteRequest(WriteRequest writeRequest) {

    }

    @Override
    public void suspendRead() {

    }

    @Override
    public void suspendWrite() {

    }

    @Override
    public void resumeRead() {

    }

    @Override
    public void resumeWrite() {

    }

    @Override
    public boolean isReadSuspended() {
        return false;
    }

    @Override
    public boolean isWriteSuspended() {
        return false;
    }

    @Override
    public void updateThroughput(long l, boolean b) {

    }

    @Override
    public long getReadBytes() {
        return 0;
    }

    @Override
    public long getWrittenBytes() {
        return 0;
    }

    @Override
    public long getReadMessages() {
        return 0;
    }

    @Override
    public long getWrittenMessages() {
        return 0;
    }

    @Override
    public double getReadBytesThroughput() {
        return 0;
    }

    @Override
    public double getWrittenBytesThroughput() {
        return 0;
    }

    @Override
    public double getReadMessagesThroughput() {
        return 0;
    }

    @Override
    public double getWrittenMessagesThroughput() {
        return 0;
    }

    @Override
    public int getScheduledWriteMessages() {
        return 0;
    }

    @Override
    public long getScheduledWriteBytes() {
        return 0;
    }

    @Override
    public Object getCurrentWriteMessage() {
        return null;
    }

    @Override
    public WriteRequest getCurrentWriteRequest() {
        return null;
    }

    @Override
    public long getCreationTime() {
        return 0;
    }

    @Override
    public long getLastIoTime() {
        return 0;
    }

    @Override
    public long getLastReadTime() {
        return 0;
    }

    @Override
    public long getLastWriteTime() {
        return 0;
    }

    @Override
    public boolean isIdle(IdleStatus idleStatus) {
        return false;
    }

    @Override
    public boolean isReaderIdle() {
        return false;
    }

    @Override
    public boolean isWriterIdle() {
        return false;
    }

    @Override
    public boolean isBothIdle() {
        return false;
    }

    @Override
    public int getIdleCount(IdleStatus idleStatus) {
        return 0;
    }

    @Override
    public int getReaderIdleCount() {
        return 0;
    }

    @Override
    public int getWriterIdleCount() {
        return 0;
    }

    @Override
    public int getBothIdleCount() {
        return 0;
    }

    @Override
    public long getLastIdleTime(IdleStatus idleStatus) {
        return 0;
    }

    @Override
    public long getLastReaderIdleTime() {
        return 0;
    }

    @Override
    public long getLastWriterIdleTime() {
        return 0;
    }

    @Override
    public long getLastBothIdleTime() {
        return 0;
    }

	@Override
	public boolean isSecured() {
		// TODO Auto-generated method stub
		return false;
	}
}
