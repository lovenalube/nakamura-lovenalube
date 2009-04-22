/*
 * Licensed to the Sakai Foundation (SF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The SF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.sakaiproject.kernel.messaging;

import org.sakaiproject.kernel.api.messaging.DateUtils;
import org.sakaiproject.kernel.api.messaging.EmailMessage;
import org.sakaiproject.kernel.api.messaging.Message;
import org.sakaiproject.kernel.api.messaging.MessageConverter;
import org.sakaiproject.kernel.api.messaging.MessagingConstants;
import org.sakaiproject.kernel.api.messaging.MessagingException;
import org.sakaiproject.kernel.api.messaging.MessagingService;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;

import javax.jcr.Node;
import javax.jcr.RepositoryException;
import javax.jcr.Session;

/**
 * Messaging service implementation that is backed by JCR.
 *
 * @scr.component description="Messaging service implementation backed by JCR"
 * @scr.service
 */
public class JcrMessagingService implements MessagingService {
  /** @scr.reference */
  private MessageConverter messageConverter;

  /** @scr.reference */
  private UserFactoryService userFactory;

  /** @scr.reference */
  private Session session;

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.messaging.MessagingService#send(javax.jms.Message)
   */
  public void send(Message msg) throws MessagingException {
    // establish the send date and add it to the message
    String date = DateUtils.rfc2822();
    msg.setHeader(Message.Field.DATE, date);

    try {
      // convert message to the storage format (json)
      String json = messageConverter.toString(msg);

      // create an input stream to the content and write to JCR
      ByteArrayInputStream bais = new ByteArrayInputStream(json.getBytes("UTF-8"));

      // get the path for the outbox
      String path = userFactory.getMessagesPath(msg.getFrom()) + "/"
          + MessagingConstants.FOLDER_OUTBOX + "/";

      // create a sha-1 hash of the content to use as the message name
      String msgName = org.sakaiproject.kernel.util.StringUtils.sha1Hash(json);

      // write the data to the node
      Node n = jcrNodeFactory.setInputStream(path + msgName, bais, "application/json");

      // set the type, recipients and date as node properties
      n.setProperty(MessagingConstants.JCR_MESSAGE_TYPE, msg.getType());
      n.setProperty(MessagingConstants.JCR_MESSAGE_FROM, msg.getFrom());
      n.setProperty(MessagingConstants.JCR_MESSAGE_RCPTS, msg.getTo());
      n.setProperty(MessagingConstants.JCR_MESSAGE_DATE, date);
    } catch (RepositoryException e) {
      throw new MessagingException(e.getMessage(), e);
    } catch (IOException e) {
      throw new MessagingException(e.getMessage(), e);
    } catch (NoSuchAlgorithmException e) {
      throw new MessagingException(e.getMessage(), e);
    }
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.messaging.MessagingService#createEmailMessage()
   */
  public EmailMessage createEmailMessage() {
    EmailMessageImpl em = new EmailMessageImpl(this);
    return em;
  }

  /**
   * {@inheritDoc}
   *
   * @see org.sakaiproject.kernel.api.messaging.MessagingService#createMessage()
   */
  public Message createMessage() {
    MessageImpl m = new MessageImpl(this);
    return m;
  }
}