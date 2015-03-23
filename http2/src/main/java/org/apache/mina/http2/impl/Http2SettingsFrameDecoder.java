/**
 * 
 */
package org.apache.mina.http2.impl;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Collection;

import org.apache.mina.http2.api.Http2FrameHeadePartialDecoder.Http2FrameHeader;
import org.apache.mina.http2.api.Http2SettingsFrame.Builder;
import org.apache.mina.http2.api.Http2Setting;
import org.apache.mina.http2.api.LongPartialDecoder;

/**
 * @author jeffmaury
 *
 */
public class Http2SettingsFrameDecoder extends Http2FrameDecoder {

    private int remaining;
    
    private LongPartialDecoder decoder = new LongPartialDecoder(6);
    
    private Builder builder = new Builder();
    
    private Collection<Http2Setting> settings = new ArrayList<Http2Setting>();
    
    public Http2SettingsFrameDecoder(Http2FrameHeader header) {
        super(header);
        remaining = header.getLength() / 6;
        initBuilder(builder);
    }
    
    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#consume(java.nio.ByteBuffer)
     */
    @Override
    public boolean consume(ByteBuffer buffer) {
        while ((getValue() == null) && buffer.remaining() > 0) {
            if (decoder.consume(buffer)) {
                remaining--;
                Http2Setting setting = new Http2Setting();
                setting.setID((int) ((decoder.getValue() & 0x00FFFF00000000L) >> 32));
                setting.setValue(decoder.getValue() & 0x00FFFFFFFFL);
                settings.add(setting);
                decoder.reset();
                if (remaining == 0) {
                    builder.settings(settings);
                    setValue(builder.build());
                }
            }
        }
        return getValue() != null;
    }

    /* (non-Javadoc)
     * @see org.apache.mina.http2.api.PartialDecoder#reset()
     */
    @Override
    public void reset() {
    }

}
