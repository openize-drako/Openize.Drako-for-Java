package dev.fileformat.drako;
class SequentialAttributeDecodersController extends AttributesDecoder
{    
    private SequentialAttributeDecoder[] sequentialDecoders;
    private int[] pointIds;
    private PointsSequencer sequencer;
    public SequentialAttributeDecodersController(PointsSequencer sequencer)
    {
        this.sequencer = sequencer;
    }
    
    @Override
    public void decodeAttributesDecoderData(DecoderBuffer buffer)
        throws DrakoException
    {
        
        super.decodeAttributesDecoderData(buffer);
        // Decode unique ids of all sequential encoders and create them.
        this.sequentialDecoders = new SequentialAttributeDecoder[this.getNumAttributes()];
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            byte decoderType = buffer.decodeU8();
            // Create the decoder from the id.
            sequentialDecoders[i] = this.createSequentialDecoder((int)(0xff & decoderType));
            if (sequentialDecoders[i] == null)
                throw DracoUtils.failed();
            sequentialDecoders[i].initialize(this.getDecoder(), this.getAttributeId(i));
        }
        
    }
    
    @Override
    public void decodeAttributes(DecoderBuffer buffer)
        throws DrakoException
    {
        
        if (sequencer == null)
            throw DracoUtils.failed();
        this.pointIds = sequencer.generateSequence();
        // Initialize point to attribute value mapping for all decoded attributes.
        for (int i = 0; i < this.getNumAttributes(); ++i)
        {
            PointAttribute pa = this.getDecoder().getPointCloud().attribute(this.getAttributeId(i));
            sequencer.updatePointToAttributeIndexMapping(pa);
        }
        
        super.decodeAttributes(buffer);
    }
    
    @Override
    protected void decodePortableAttributes(DecoderBuffer buffer)
        throws DrakoException
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            sequentialDecoders[i].decodePortableAttribute(pointIds, buffer);
        }
        
    }
    
    @Override
    protected void decodeDataNeededByPortableTransforms(DecoderBuffer buffer)
        throws DrakoException
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            sequentialDecoders[i].decodeDataNeededByPortableTransform(pointIds, buffer);
        }
        
    }
    
    @Override
    protected void transformAttributesToOriginalFormat()
        throws DrakoException
    {
        int num_attributes = this.getNumAttributes();
        for (int i = 0; i < num_attributes; ++i)
        {
            // Check whether the attribute transform should be skipped.
            if (this.getDecoder().options != null)
            {
                PointAttribute attribute = sequentialDecoders[i].getAttribute();
                if (this.getDecoder().options.skipAttributeTransform)
                {
                    // Attribute transform should not be performed. In this case, we replace
                    // the output geometry attribute with the portable attribute.
                    // TODO(ostava): We can potentially avoid this copy by introducing a new
                    // mechanism that would allow to use the final attributes as portable
                    // attributes for predictors that may need them.
                    sequentialDecoders[i].getAttribute().copyFrom(sequentialDecoders[i].getPortableAttribute());
                    continue;
                }
                
            }
            
            
            sequentialDecoders[i].transformAttributeToOriginalFormat(pointIds);
        }
        
    }
    
    protected SequentialAttributeDecoder createSequentialDecoder(int decoderType)
    {
        switch(decoderType)
        {
            case SequentialAttributeEncoderType.GENERIC:
                return new SequentialAttributeDecoder();
            case SequentialAttributeEncoderType.INTEGER:
                return new SequentialIntegerAttributeDecoder();
            case SequentialAttributeEncoderType.QUANTIZATION:
                return new SequentialQuantizationAttributeDecoder();
            case SequentialAttributeEncoderType.NORMALS:
                return new SequentialNormalAttributeDecoder();
            default:
                return null;
        }
        
    }
    
    @Override
    public PointAttribute getPortableAttribute(int attId)
    {
        int loc_id = this.getLocalIdForPointAttribute(attId);
        if (loc_id < 0)
            return null;
        return sequentialDecoders[loc_id].getPortableAttribute();
    }
    
}
