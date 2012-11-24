/*
 * Copyright (c) 2007-2012 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.concurrentinc.com/
 */

package pattern;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.xml.xpath.XPathConstants;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import cascading.tuple.Tuple;
import pattern.tree.Context;
import pattern.tree.Edge;
import pattern.tree.Tree;
import pattern.tree.TreeModel;


public class Segmentation extends Model implements Serializable
  {
  /** Field LOG */
  private static final Logger LOG = LoggerFactory.getLogger( Segmentation.class );

  public Context context;
  public List<Model> segments = new ArrayList<Model>();
  public Map<String, Integer> votes = new HashMap<String, Integer>();

  /**
   * @param pmml PMML model
   * @throws PatternException
   */
  public Segmentation( PMML pmml ) throws PatternException
    {
    this.schema = pmml.getSchema();
    this.context = new Context();

    schema.parseMiningSchema( pmml.getNodeList( "/PMML/MiningModel/MiningSchema/MiningField" ) );

    String expr = "/PMML/MiningModel/Segmentation/Segment";
    NodeList node_list = pmml.getNodeList( expr );

    for( int i = 0; i < node_list.getLength(); i++ )
      {
      Node node = node_list.item( i );

      if( node.getNodeType() == Node.ELEMENT_NODE )
        {
        TreeModel tree_model = new TreeModel( pmml, node, context );
        segments.add( tree_model );
        }
      }
    }

  /**
   * Prepare to classify with this model. Called immediately before
   * the enclosing Operation instance is put into play processing
   * Tuples.
   */
  @Override
  public void prepare()
    {
    context.prepare( schema );
    }

  /**
   * Classify an input tuple, returning the predicted label.
   *
   * @param values tuple values
   * @return String
   * @throws PatternException
   */
  @Override
  public String classifyTuple( Tuple values ) throws PatternException
    {
    Boolean[] pred_eval = context.evalPredicates( schema, values );
    String label = null;
    Integer winning_vote = 0;

    votes.clear();

    // tally the vote for each tree in the forest

    for( Model model : segments )
      {
      label = ((TreeModel) model).tree.traverse( pred_eval );

      if( !votes.containsKey( label ) )
        winning_vote = 1;
      else
        winning_vote = votes.get( label ) + 1;

      votes.put( label, winning_vote );
      }

    // determine the winning label

    for( String key : votes.keySet() )
      {
      if( votes.get( key ) > winning_vote )
        {
        label = key;
        winning_vote = votes.get( key );
        }
      }

    return label;
    }

  /** @return String  */
  @Override
  public String toString()
    {
    StringBuilder buf = new StringBuilder();

    buf.append( "segments: " );
    buf.append( segments );
    buf.append( "---------" );
    buf.append( "\n" );

    for( Model model : segments )
      {
      Tree tree = ((TreeModel) model).tree;
      buf.append( tree );
      buf.append( tree.getRoot() );

      for( Edge edge : tree.getGraph().edgeSet() )
        buf.append( edge );

      buf.append( "\n" );
      }

    buf.append( "---------" );
    buf.append( "\n" );
    buf.append( "votes: " );
    buf.append( votes );

    return buf.toString();
    }
  }