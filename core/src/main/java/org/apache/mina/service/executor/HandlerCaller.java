package org.apache.mina.service.executor;

import org.apache.mina.api.IoSession;

class HandlerCaller implements EventVisitor {

    @Override
    public void visit(CloseEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionClosed(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(IdleEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionIdle(session, event.getIdleStatus());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }

    }

    @Override
    public void visit(OpenEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().sessionOpened(session);
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }

    }

    @Override
    public void visit(ReceiveEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().messageReceived(session, event.getMessage());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }

    @Override
    public void visit(SentEvent event) {
        IoSession session = event.getSession();
        try {
            session.getService().getIoHandler().messageSent(session, event.getMessage());
        } catch (Exception e) {
            session.getService().getIoHandler().exceptionCaught(session, e);
        }
    }
}