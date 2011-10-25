package org.apache.mina.ldap;

import java.nio.ByteBuffer;

import org.apache.directory.shared.ldap.codec.api.LdapApiService;
import org.apache.directory.shared.ldap.codec.api.LdapApiServiceFactory;
import org.apache.directory.shared.ldap.codec.api.LdapMessageContainer;
import org.apache.directory.shared.ldap.codec.api.MessageDecorator;
import org.apache.directory.shared.ldap.model.message.AddResponse;
import org.apache.directory.shared.ldap.model.message.BindResponse;
import org.apache.directory.shared.ldap.model.message.CompareResponse;
import org.apache.directory.shared.ldap.model.message.DeleteResponse;
import org.apache.directory.shared.ldap.model.message.ExtendedResponse;
import org.apache.directory.shared.ldap.model.message.IntermediateResponse;
import org.apache.directory.shared.ldap.model.message.Message;
import org.apache.directory.shared.ldap.model.message.ModifyDnResponse;
import org.apache.directory.shared.ldap.model.message.ModifyResponse;
import org.apache.directory.shared.ldap.model.message.SearchResultDone;
import org.apache.directory.shared.ldap.model.message.SearchResultEntry;
import org.apache.directory.shared.ldap.model.message.SearchResultReference;
import org.apache.mina.api.IdleStatus;
import org.apache.mina.api.IoSession;
import org.apache.mina.filter.codec.ProtocolCodecFilter;
import org.apache.mina.filter.codec.ProtocolDecoder;
import org.apache.mina.filter.codec.ProtocolEncoder;
import org.apache.mina.filterchain.WriteFilterChainController;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class LdapCodec extends ProtocolCodecFilter {
    private static final Logger LOG = LoggerFactory.getLogger(LdapCodec.class);

    /** The LDAP decoder instance */
    private static ProtocolDecoder ldapDecoder = new LdapProtocolDecoder();

    /** The LDAP encoder instance */
    private static ProtocolEncoder ldapEncoder = new LdapProtocolEncoder();

    /** The codec */
    private static LdapApiService codec = LdapApiServiceFactory.getSingleton();

    public LdapCodec() {
        super(ldapEncoder, ldapDecoder);
    }
    
    
    @Override
    public void sessionCreated(IoSession session) {
        LdapMessageContainer<MessageDecorator<? extends Message>> container = 
            new LdapMessageContainer<MessageDecorator<? extends Message>>(codec);
        session.setAttribute("messageContainer", container);
    }
    

    @Override
    public void sessionOpened(IoSession session) {
    }
    

    @Override
    public void sessionClosed(IoSession session) {
        session.removeAttribute( "messageContainer" );
    }
    

    @Override
    public void sessionIdle(IoSession session, IdleStatus status) {
        // TODO Auto-generated method stub
    }
    

    @Override
    public void messageWriting(IoSession session, Object message, WriteFilterChainController controller) {
        if (message instanceof AddResponse) {
            ldapEncoder.encode(session, (AddResponse)message, controller);
        } else if (message instanceof BindResponse) {
            ldapEncoder.encode(session, (BindResponse)message, controller);
        } else if (message instanceof DeleteResponse) {
            ldapEncoder.encode(session, (DeleteResponse)message, controller);
        } else if (message instanceof CompareResponse) {
            ldapEncoder.encode(session, (CompareResponse)message, controller);
        } else if (message instanceof ExtendedResponse) {
            ldapEncoder.encode(session, (ExtendedResponse)message, controller);
        } else if (message instanceof IntermediateResponse) {
            ldapEncoder.encode(session, (IntermediateResponse)message, controller);
        } else if (message instanceof ModifyResponse) {
            ldapEncoder.encode(session, (ModifyResponse)message, controller);
        } else if (message instanceof ModifyDnResponse) {
            ldapEncoder.encode(session, (ModifyDnResponse)message, controller);
        } else if (message instanceof SearchResultDone) {
            ldapEncoder.encode(session, (SearchResultDone)message, controller);
        } else if (message instanceof SearchResultEntry) {
            ldapEncoder.encode(session, (SearchResultEntry)message, controller);
        } else if (message instanceof SearchResultReference) {
            ldapEncoder.encode(session, (SearchResultReference)message, controller);
        } else if (message instanceof ByteBuffer) {
            controller.callWriteNextFilter(session, message);
        }
    }
}