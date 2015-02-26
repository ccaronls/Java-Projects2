package cc.fantasy.xml;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Random;

import cc.lib.crypt.Cypher;
import cc.lib.crypt.EncryptionInputStream;
import cc.lib.crypt.EncryptionOutputStream;
import cc.lib.crypt.HuffmanEncoding;

public class FantasyServiceXmlDBEncrypted extends FantasyServiceXmlDB {

    Cypher cypher;
    
    static Cypher getCypher() {
        HuffmanEncoding huffman = new HuffmanEncoding(); 
        Random r = new Random(231786198);
        for (int i=10; i<128; i++) {
            huffman.setCount(i, (Math.abs(r.nextInt()) % 1000) + 1000);
        }
        try {
            huffman.generate(true);
        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Huffman failed : " + e.getMessage());
        }
        return huffman;
    }
    
    
    public FantasyServiceXmlDBEncrypted(String dbDir) {
        super(dbDir);
        cypher = getCypher();
    }


    @Override
    protected InputStream getFranchiseDBInputStream() throws Exception {
        return new EncryptionInputStream(new FileInputStream(getFranchiseDB()), cypher, false);
    }


    @Override
    protected OutputStream getFranchiseOutputStream() throws Exception {
        return new EncryptionOutputStream(new FileOutputStream(getFranchiseDB()), cypher);
    }


    @Override
    protected InputStream getUserDBInputStream() throws Exception {
        return new EncryptionInputStream(new FileInputStream(getUserDB()), cypher, false);
    }


    @Override
    protected OutputStream getUserDBOutputStream() throws Exception {
        return new EncryptionOutputStream(new FileOutputStream(getUserDB()), cypher);
    }


	@Override
	protected InputStream getLeagueDBInputStream() throws Exception {
		return new EncryptionInputStream(new FileInputStream(getLeagueDB()), cypher, false);
	}


	@Override
	protected OutputStream getLeagueOutputStream() throws Exception {
		return new EncryptionOutputStream(new FileOutputStream(getLeagueDB()), cypher);
	}


    @Override
    protected InputStream getTeamDBInputStream() throws Exception {
        return new EncryptionInputStream(new FileInputStream(getTeamDB()), cypher, false);
    }


    @Override
    protected OutputStream getTeamDBOutputStream() throws Exception {
        return new EncryptionOutputStream(new FileOutputStream(getTeamDB()), cypher);
    }


	@Override
	protected String getFileExtension() {
		return "xml.encrypted";
	}

    
    
}
