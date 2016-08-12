<xsl:stylesheet version="1.0" 
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:diversicon="https://github.com/DavidLeoni/diversicon" exclude-result-prefixes="diversicon">


	<xsl:output method="xml" encoding="utf-8" indent="true"/>						
			
			
			
	  <diversicon:posMap>
	    <entry key="n">noun</entry>
	    <entry key="v">verb</entry>
	    <entry key="a">adjective</entry>
	    <entry key="r">adverb</entry>
	  </diversicon:posMap>
  
  
	<diversicon:synsetRelationRelType>

	    <entry key="hmem"></entry>
	    <entry key="attr"></entry>
	    <entry key="ants"></entry> 
	    
	    <entry key="mmem">Member meronym</entry>
	    <entry key="mprt">Part meronym</entry>
	    <entry key="msub">Substance meronym</entry>
	    
	    <entry key="dmnr"></entry>
	    <entry key="dmnc"></entry>
	    <entry key="dmnu"></entry>
	    <entry key="dmtc"></entry>
		<entry key="dmtr"></entry>	    
	    <entry key="dmtu"></entry>
	    
	    <entry key="hype">hypernym</entry>
	    <entry key="inst"></entry>
	    <entry key="hypo">hyponym</entry>
	    	    
	    <entry key="hprt"></entry>
	    <entry key="hsub"></entry>
	    <entry key="hasi"></entry>
	    
	    <entry key="caus"></entry>
	    <entry key="sim"></entry>	    
	    <entry key="also"></entry>
	    
	</diversicon:synsetRelationRelType>			
			
 
  

	<xsl:template match="/">
	
	
	<xsl:output indent="yes"/>
	


		<xsl:for-each select="LexicalResource">
			<LexicalResource name="{GlobalInformation/@label}">
	

	
	
				<GlobalInformation label="{GlobalInformation/@label}"/>
	
				<xsl:for-each select="Lexicon">
	
					<xsl:variable name="languageCoding" select="@languageCoding"/>
	
					<Lexicon id="{/LexicalResource/GlobalInformation/@label}_{@languageCoding}" name="{@label}" languageIdentifier="{@languageCoding}">
	
						<xsl:for-each select="LexicalEntry">
	 						<xsl:variable name="pos" select="Lemma/@partOfSpeech"/>
	 					
							<LexicalEntry id="{@id}" 
									      partOfSpeech="{document('')/*/diversicon:posMap/entry[@key=$pos]}">
								<Lemma>
									<FormRepresentation languageIdentifier="{$languageCoding}" writtenForm="{Lemma/@writtenForm}">										
									</FormRepresentation>
								</Lemma>

								<xsl:for-each select="Sense">	
						 			<Sense id="{@id}" synset="{@synset}">
						            </Sense>
						            
					            </xsl:for-each>
								
							</LexicalEntry>
	
						</xsl:for-each>
						
						<xsl:for-each select="Synset">
							
							<Synset id="{@id}"> <!--  todo map basicConcept -->

								<xsl:for-each select="Definition">
									<Definition>
										<TextRepresentation languageIdentifier="{$languageCoding}"
															writtenText="{@gloss}">										
										</TextRepresentation>
									</Definition>
									<xsl:for-each select="Statement[@key='example']">
										<Statement statementType="usageNote">
											<TextRepresentation languageIdentifier="{$languageCoding}"
																writtenText="{@example}">
											</TextRepresentation>						
										</Statement>
									</xsl:for-each>
								</xsl:for-each>						
																				
								<xsl:for-each select="SynsetRelations">
									<xsl:for-each select="SynsetRelation">
										<SynsetRelation target="{@targets}" relType="" relName="">
												
										</SynsetRelation>
									</xsl:for-each>														
								</xsl:for-each>						
							
							
							</Synset>
							
						</xsl:for-each>
	
	
					</Lexicon>
				</xsl:for-each>
	
			</LexicalResource>
		</xsl:for-each>
	</xsl:template>

</xsl:stylesheet>
						